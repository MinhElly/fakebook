package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.client.FeedReadClient;
import com.minh.fakebook.feed.client.SocialContextClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Server-owned source cursors and replayable pages; no client-side source merging. */
@Service
public class HomeFeedService {
    private static final List<String> SOURCES = List.of("FRIENDS", "FOLLOWING", "DISCOVERY");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final FeedReadClient posts;
    private final SocialContextClient social;
    private final String signingKey;

    public HomeFeedService(JdbcTemplate jdbc, ObjectMapper mapper, FeedReadClient posts, SocialContextClient social,
            @Value("${application.feed.cursor-secret:}") String signingKey) {
        this.jdbc = jdbc; this.mapper = mapper; this.posts = posts; this.social = social;
        if (signingKey == null || signingKey.length() < 32 || signingKey.startsWith("REPLACE_ME")) throw new IllegalArgumentException("Configure application.feed.cursor-secret with at least 32 characters");
        this.signingKey = signingKey;
    }

    public record Item(UUID postId, String source) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(List<Item> items, String nextCursor, boolean hasMore) {}
    public static class State {
        public UUID viewerId;
        public String viewer;
        public String mode;
        public Instant asOf;
        public long seed;
        public Set<UUID> friends = new HashSet<>();
        public Set<UUID> following = new HashSet<>();
        public Map<String, String> cursors = new HashMap<>();
        public Set<String> exhausted = new HashSet<>();
        public int nextPage;
        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<FeedReadClient.Candidate> projection;
    }

    @Transactional
    public Page get(UUID viewerId, String mode, String cursor) {
        if (viewerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String viewer = viewerId.toString();
        UUID sessionId;
        int pageNumber;
        State state;
        if (cursor == null || cursor.isBlank()) {
            state = new State(); state.viewerId = viewerId; state.viewer = viewer; state.mode = mode;
            state.asOf = Instant.now(); state.seed = new java.security.SecureRandom().nextLong();
            if (viewerId != null) {
                var relationships = social.read(viewerId);
                if (relationships == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Social context unavailable");
                state.friends.addAll(relationships.friends()); state.following.addAll(relationships.following());
            }
            if (viewerId == null) state.exhausted.addAll(List.of("FRIENDS", "FOLLOWING"));
            if (mode.equals("friends")) state.exhausted.addAll(List.of("FOLLOWING", "DISCOVERY"));
            sessionId = UUID.randomUUID(); pageNumber = 0;
            jdbc.update("INSERT INTO home_feed_session (id, viewer, mode, created_at, state) VALUES (?, ?, ?, ?, ?)",
                sessionId.toString(), viewer, mode, state.asOf.toString(), mapper.writeValueAsString(state));
        } else {
            String[] parts = verify(cursor, viewer, mode);
            sessionId = UUID.fromString(parts[0]); pageNumber = Integer.parseInt(parts[1]);
            var rows = jdbc.queryForList("SELECT state FROM home_feed_session WHERE id = ? AND viewer = ? AND mode = ? FOR UPDATE",
                String.class, sessionId.toString(), viewer, mode);
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.GONE, "Feed session expired");
            state = mapper.readValue(rows.getFirst(), State.class);
            if (state.asOf.plus(Duration.ofMinutes(30)).isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Feed session expired");
        }
        var current = viewerId == null ? null : social.read(viewerId);
        if (viewerId != null && current == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        var currentFriends = current == null ? Set.<UUID>of() : current.friends();
        var currentFollowing = current == null ? Set.<UUID>of() : current.following();
        var cached = jdbc.queryForList("SELECT payload FROM home_feed_page WHERE session_id = ? AND page_number = ?", String.class, sessionId.toString(), pageNumber);
        if (!cached.isEmpty()) {
            Page old = mapper.readValue(cached.getFirst(), Page.class);
            Set<UUID> readable = allowed(old.items(), viewerId, currentFriends, currentFollowing, mode);
            List<Item> replay = new ArrayList<>(old.items().stream().filter(i -> readable.contains(i.postId())).toList());
            if (replay.size() == old.items().size()) return old;
            int budget = old.items().size();
            for (String source : SOURCES) {
                long missing = old.items().stream().filter(i -> i.source().equals(source) && !readable.contains(i.postId())).count();
                take(state, sessionId, source, Math.min((int) missing, budget - replay.size()), replay,
                    currentFriends, currentFollowing);
            }
            fill(state, sessionId, budget, replay, currentFriends, currentFollowing);
            boolean more = old.hasMore() || state.exhausted.size() < 3;
            Page repaired = new Page(List.copyOf(replay), more ? sign(sessionId, pageNumber + 1, viewer, mode) : null, more);
            jdbc.update("UPDATE home_feed_session SET state = ? WHERE id = ?", mapper.writeValueAsString(state), sessionId.toString());
            jdbc.update("UPDATE home_feed_page SET payload = ? WHERE session_id = ? AND page_number = ?", mapper.writeValueAsString(repaired), sessionId.toString(), pageNumber);
            return repaired;
        }
        if (pageNumber != state.nextPage) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor is out of sequence");
        int budget = 11;
        List<Item> selected = new ArrayList<>();
        int[] quotas = mode.equals("friends") ? new int[]{11, 0, 0} : new int[]{5, 2, 4};
        for (int source = 0; source < SOURCES.size(); source++) {
            take(state, sessionId, SOURCES.get(source), Math.min(quotas[source], budget - selected.size()), selected,
                currentFriends, currentFollowing);
        }
        fill(state, sessionId, budget, selected, currentFriends, currentFollowing);
        if (mode.equals("home")) Collections.shuffle(selected, new Random(state.seed ^ pageNumber));
        boolean more = state.exhausted.size() < 3;
        state.nextPage++;
        Page result = new Page(List.copyOf(selected), more ? sign(sessionId, state.nextPage, viewer, mode) : null, more);
        jdbc.update("UPDATE home_feed_session SET state = ? WHERE id = ?", mapper.writeValueAsString(state), sessionId.toString());
        jdbc.update("INSERT INTO home_feed_page (session_id, page_number, payload) VALUES (?, ?, ?)", sessionId.toString(), pageNumber, mapper.writeValueAsString(result));
        return result;
    }

    private void fill(State state, UUID session, int budget, List<Item> selected,
            Set<UUID> friends, Set<UUID> following) {
        while (selected.size() < budget && state.exhausted.size() < 3) {
            for (String source : SOURCES) {
                if (selected.size() < budget) take(state, session, source, 1, selected, friends, following);
            }
        }
    }

    private void take(State state, UUID session, String source, int wanted, List<Item> result,
            Set<UUID> currentFriends, Set<UUID> currentFollowing) {
        int accepted = 0;
        while (accepted < wanted && !state.exhausted.contains(source)) {
            // Fetch only what this quota can consume, so no unconsumed candidates are skipped.
            var response = sourceCandidates(state, source, wanted - accepted);
            if (response == null || response.items() == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post candidates unavailable");
            if (response.hasMore() && (response.nextCursor() == null || response.nextCursor().equals(state.cursors.get(source)))) {
                throw new IllegalStateException("Candidate cursor did not advance");
            }
            state.cursors.put(source, response.nextCursor());
            if (!response.hasMore()) state.exhausted.add(source);
            List<Item> items = response.items().stream().map(p -> new Item(p.postId(), source)).toList();
            Set<UUID> allowed = allowed(items, state.viewerId, currentFriends, currentFollowing, state.mode);
            for (var candidate : response.items()) {
                if (!allowed.contains(candidate.postId())) continue;
                int inserted = jdbc.update("INSERT IGNORE INTO home_feed_seen (session_id, post_id) VALUES (?, ?)", session.toString(), candidate.postId().toString());
                if (inserted == 0) continue;
                if (state.viewerId != null && !source.equals("DISCOVERY")) {
                    // Reconcile authoritative original timestamps and fill missing fan-out for new connections.
                    jdbc.update("INSERT INTO feed_items (id, user_id, post_id, author_id, visibility, created_at) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE created_at = VALUES(created_at), visibility = VALUES(visibility), author_id = VALUES(author_id)",
                        UUID.randomUUID().toString(), state.viewerId.toString(), candidate.postId().toString(), candidate.authorId().toString(), candidate.visibility(), java.sql.Timestamp.from(candidate.createdAt()));
                }
                result.add(new Item(candidate.postId(), source)); accepted++;
            }
        }
    }

    /** Merge the fan-out projection with authoritative backfill for newly connected authors. */
    private FeedReadClient.CandidatePage sourceCandidates(State state, String source, int limit) {
        var fresh = posts.candidates(new FeedReadClient.CandidateRequest(state.viewerId, state.friends, state.following,
            source, state.asOf, state.cursors.get(source), limit));
        if (fresh == null || state.viewerId == null || source.equals("DISCOVERY")) return fresh;
        if (state.projection == null) state.projection = reconcileProjection(state);
        var projected = state.projection;
        Map<UUID, FeedReadClient.Candidate> merged = new HashMap<>();
        fresh.items().forEach(p -> merged.put(p.postId(), p));
        String boundary = state.cursors.get(source);
        for (var post : projected) {
            boolean friend = post.authorId().equals(state.viewerId) || state.friends.contains(post.authorId());
            boolean member = source.equals("FRIENDS") ? friend : !friend && state.following.contains(post.authorId()) && post.visibility().equals("PUBLIC");
            if (!member || post.createdAt().isAfter(state.asOf) || !before(post, boundary)) continue;
            merged.put(post.postId(), post);
        }
        var ordered = merged.values().stream().sorted(Comparator.comparing(FeedReadClient.Candidate::createdAt)
            .thenComparing(p -> p.postId().toString()).reversed()).toList();
        var selected = ordered.stream().limit(limit).toList();
        boolean more = fresh.hasMore() || ordered.size() > limit;
        return new FeedReadClient.CandidatePage(selected, more && !selected.isEmpty() ? candidateCursor(selected.getLast()) : null, more);
    }

    /** Reconcile once per HTTP request; unchanged rows require no write. Authorization is still checked for each page. */
    private List<FeedReadClient.Candidate> reconcileProjection(State state) {
        var existing = jdbc.query("SELECT post_id, author_id, visibility, created_at FROM feed_items WHERE user_id = ? AND created_at <= ? " +
            "ORDER BY created_at DESC, post_id DESC LIMIT 100", (rs, row) -> new FeedReadClient.Candidate(
                UUID.fromString(rs.getString("post_id")), UUID.fromString(rs.getString("author_id")), rs.getString("visibility"), rs.getTimestamp("created_at").toInstant()),
            state.viewerId.toString(), java.sql.Timestamp.from(state.asOf));
        if (existing.isEmpty()) return List.of();
        var readable = posts.readable(new FeedReadClient.ReadRequest(existing.stream().map(FeedReadClient.Candidate::postId).toList(), state.viewerId, state.friends));
        if (readable == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Projection reconciliation unavailable");
        Map<UUID, FeedReadClient.Candidate> old = new HashMap<>();
        existing.forEach(p -> old.put(p.postId(), p));
        List<Object[]> repairs = new ArrayList<>();
        for (var post : readable) {
            if (!post.equals(old.get(post.postId()))) repairs.add(new Object[]{java.sql.Timestamp.from(post.createdAt()), post.authorId().toString(), post.visibility(), state.viewerId.toString(), post.postId().toString()});
        }
        if (!repairs.isEmpty()) jdbc.batchUpdate("UPDATE feed_items SET created_at = ?, author_id = ?, visibility = ? WHERE user_id = ? AND post_id = ?", repairs);
        return readable;
    }

    private boolean before(FeedReadClient.Candidate post, String cursor) {
        if (cursor == null) return true;
        String[] boundary = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", 2);
        int timeOrder = post.createdAt().compareTo(Instant.parse(boundary[0]));
        return timeOrder < 0 || timeOrder == 0 && post.postId().toString().compareTo(boundary[1]) < 0;
    }

    private String candidateCursor(FeedReadClient.Candidate post) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((post.createdAt() + "|" + post.postId()).getBytes(StandardCharsets.UTF_8));
    }

    private Set<UUID> allowed(List<Item> items, UUID viewer, Set<UUID> friends, Set<UUID> following, String mode) {
        if (items.isEmpty()) return Set.of();
        var readable = posts.readable(new FeedReadClient.ReadRequest(items.stream().map(Item::postId).toList(), viewer, friends));
        if (readable == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post authorization unavailable");
        Map<UUID, FeedReadClient.Candidate> byId = new HashMap<>(); readable.forEach(p -> byId.put(p.postId(), p));
        Set<UUID> result = new HashSet<>();
        for (Item item : items) {
            var p = byId.get(item.postId()); if (p == null) continue;
            boolean friend = p.authorId().equals(viewer) || friends.contains(p.authorId());
            boolean followed = following.contains(p.authorId());
            if (mode.equals("friends") && !friend) continue;
            if (item.source().equals("FRIENDS") && !friend) continue;
            if (item.source().equals("FOLLOWING") && (!followed || friend || !p.visibility().equals("PUBLIC"))) continue;
            if (item.source().equals("DISCOVERY") && (friend || followed || !p.visibility().equals("PUBLIC"))) continue;
            result.add(item.postId());
        }
        return result;
    }

    private String sign(UUID session, int page, String viewer, String mode) {
        String raw = session + ":" + page;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8)) + "." + signature(raw + "|" + viewer + "|" + mode);
    }
    private String[] verify(String cursor, String viewer, String mode) {
        try {
            String[] pieces = cursor.split("\\.", 2);
            String raw = new String(Base64.getUrlDecoder().decode(pieces[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(signature(raw + "|" + viewer + "|" + mode).getBytes(StandardCharsets.UTF_8), pieces[1].getBytes(StandardCharsets.UTF_8))) throw new IllegalArgumentException();
            String[] parts = raw.split(":", 2);
            UUID.fromString(parts[0]);
            if (Integer.parseInt(parts[1]) < 0) throw new IllegalArgumentException();
            return parts;
        } catch (Exception invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed cursor"); }
    }
    private String signature(String raw) {
        if (signingKey.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure application.feed.cursor-secret with at least 32 characters");
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException error) { throw new IllegalStateException(error); }
    }
}
