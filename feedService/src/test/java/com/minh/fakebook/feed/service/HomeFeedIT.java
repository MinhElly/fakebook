package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.minh.fakebook.feed.IntegrationTest;
import com.minh.fakebook.feed.client.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
@TestPropertySource(properties = "application.feed.cursor-secret=integration-test-key-at-least-32-characters")
class HomeFeedIT {
    @Autowired HomeFeedService service;
    @MockitoBean FeedReadClient posts;
    @MockitoBean SocialContextClient social;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired FeedSessionExpiry expiry;
    final UUID viewer = UUID.randomUUID(), friend = UUID.randomUUID(), followed = UUID.randomUUID(), stranger = UUID.randomUUID();
    final Map<String, List<FeedReadClient.Candidate>> sources = new HashMap<>();
    final Set<UUID> revoked = new HashSet<>();

    @BeforeEach void dependencies() {
        when(social.read(viewer)).thenReturn(new SocialContextClient.SocialContext(Set.of(friend), Set.of(friend, followed)));
        when(posts.candidates(any())).thenAnswer(call -> {
            FeedReadClient.CandidateRequest r = call.getArgument(0);
            var all = sources.getOrDefault(r.source(), List.of()).stream().filter(p -> {
                if (r.cursor() == null) return true;
                String[] parts = new String(Base64.getUrlDecoder().decode(r.cursor()), java.nio.charset.StandardCharsets.UTF_8).split("\\|", 2);
                int order = p.createdAt().compareTo(Instant.parse(parts[0]));
                return order < 0 || order == 0 && p.postId().toString().compareTo(parts[1]) < 0;
            }).toList();
            var selected = all.stream().limit(r.limit()).toList();
            String next = all.size() > r.limit() ? Base64.getUrlEncoder().withoutPadding().encodeToString(
                (selected.getLast().createdAt() + "|" + selected.getLast().postId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)) : null;
            return new FeedReadClient.CandidatePage(selected, next, all.size() > r.limit());
        });
        when(posts.readable(any())).thenAnswer(call -> {
            FeedReadClient.ReadRequest r = call.getArgument(0);
            return sources.values().stream().flatMap(Collection::stream).filter(p -> r.ids().contains(p.postId()) && !revoked.contains(p.postId())).toList();
        });
    }
    void supply(String source, int count) {
        UUID author = source.equals("FRIENDS") ? friend : source.equals("FOLLOWING") ? followed : stranger;
        List<FeedReadClient.Candidate> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) rows.add(new FeedReadClient.Candidate(UUID.randomUUID(), author, "PUBLIC", Instant.now().minusSeconds(i + 1)));
        sources.put(source, rows);
    }
    @Test void projectionIsHydratedOncePerPageAndOriginalTimestampIsRepaired() {
        supply("FRIENDS", 15); supply("FOLLOWING", 8); supply("DISCOVERY", 12);
        var projected = sources.get("FRIENDS").getFirst();
        jdbc.update("INSERT INTO feed_items (id, user_id, post_id, author_id, visibility, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            UUID.randomUUID().toString(), viewer.toString(), projected.postId().toString(), friend.toString(), "PUBLIC", java.sql.Timestamp.from(projected.createdAt().plusSeconds(1)));
        var page = service.get(viewer, "home", null);
        var projectionRequests = org.mockito.Mockito.mockingDetails(posts).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("readable"))
            .map(i -> (FeedReadClient.ReadRequest)i.getArgument(0))
            .filter(r -> r.ids().size() == 1 && r.ids().contains(projected.postId())).count();
        assertThat(projectionRequests).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT created_at FROM feed_items WHERE user_id = ? AND post_id = ?", java.sql.Timestamp.class, viewer.toString(), projected.postId().toString()).toInstant())
            .isEqualTo(projected.createdAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(page.items()).hasSize(11);
    }

    @Test void quotasReplayDedupAndViewerBinding() {
        supply("FRIENDS", 15); supply("FOLLOWING", 8); supply("DISCOVERY", 12);
        var first = service.get(viewer, "home", null);
        assertThat(first.items()).hasSize(11);
        assertThat(first.items().stream().filter(i -> i.source().equals("FRIENDS"))).hasSize(5);
        assertThat(first.items().stream().filter(i -> i.source().equals("FOLLOWING"))).hasSize(2);
        assertThat(first.items().stream().filter(i -> i.source().equals("DISCOVERY"))).hasSize(4);
        var second = service.get(viewer, "home", first.nextCursor());
        assertThat(service.get(viewer, "home", first.nextCursor())).isEqualTo(second);
        assertThat(second.items()).extracting(HomeFeedService.Item::postId).doesNotContainAnyElementsOf(first.items().stream().map(HomeFeedService.Item::postId).toList());
        assertThatThrownBy(() -> service.get(viewer, "friends", first.nextCursor())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0,3,5,6,11})
    void shortSourcesFillAndTerminateExactly(int count) {
        supply("FRIENDS", count); supply("DISCOVERY", count);
        Set<UUID> ids = new HashSet<>();
        String cursor = null; boolean more;
        do {
            var page = service.get(viewer, "home", cursor);
            for (var item : page.items()) assertThat(ids.add(item.postId())).isTrue();
            more = page.hasMore(); cursor = page.nextCursor();
        } while (more);
        assertThat(ids).hasSize(count * 2);
    }
    @Test void friendsNeverIncludesFollowingOrDiscovery() {
        supply("FRIENDS", 6); supply("FOLLOWING", 6); supply("DISCOVERY", 11);
        var page = service.get(viewer, "friends", null);
        assertThat(page.items()).hasSize(6).allMatch(i -> i.source().equals("FRIENDS"));
        assertThat(page.hasMore()).isFalse();
    }
    @Test void stalePrivacyAndUnfriendCannotLeakFromCachedPage() {
        supply("FRIENDS", 15); supply("DISCOVERY", 20);
        var first = service.get(viewer, "home", null);
        var second = service.get(viewer, "home", first.nextCursor());
        UUID removed = second.items().getFirst().postId(); revoked.add(removed);
        when(social.read(viewer)).thenReturn(new SocialContextClient.SocialContext(Set.of(), Set.of()));
        var replay = service.get(viewer, "home", first.nextCursor());
        assertThat(replay.items()).noneMatch(i -> i.postId().equals(removed) || i.source().equals("FRIENDS"));
    }
    @Test void dependencyFailureDoesNotConsumeCursor() {
        supply("DISCOVERY", 30);
        var first = service.get(viewer, "home", null);
        org.mockito.Mockito.doThrow(new IllegalStateException("temporary failure")).when(posts).candidates(any());
        assertThatThrownBy(() -> service.get(viewer, "home", first.nextCursor())).hasMessageContaining("temporary failure");
    }
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentRequestsReplayOneCommittedPage() throws Exception {
        supply("DISCOVERY", 40);
        var first = service.get(viewer, "home", null);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> service.get(viewer, "home", first.nextCursor()));
            var b = executor.submit(() -> service.get(viewer, "home", first.nextCursor()));
            var page = a.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(b.get(20, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(page);
            assertThat(page.items()).hasSize(11);
        }
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failedDependencyRollsBackAndSameCursorCanRetry() {
        supply("DISCOVERY", 30);
        var first = service.get(viewer, "home", null);
        var original = new ArrayList<>(sources.get("DISCOVERY"));
        org.mockito.Mockito.doThrow(new IllegalStateException("authorization unavailable")).when(posts).readable(any());
        assertThatThrownBy(() -> service.get(viewer, "home", first.nextCursor())).hasMessageContaining("authorization unavailable");
        org.mockito.Mockito.doAnswer(call -> {
            FeedReadClient.ReadRequest r = call.getArgument(0);
            return original.stream().filter(p -> r.ids().contains(p.postId())).toList();
        }).when(posts).readable(any());
        var retry = service.get(viewer, "home", first.nextCursor());
        assertThat(retry.items()).hasSize(11);
        assertThat(service.get(viewer, "home", first.nextCursor())).isEqualTo(retry);
    }
    @Test void cachedPageReplacesRevokedPostFromItsOriginalSource() {
        supply("FRIENDS", 20); supply("FOLLOWING", 15); supply("DISCOVERY", 20);
        var first = service.get(viewer, "home", null);
        var second = service.get(viewer, "home", first.nextCursor());
        revoked.add(second.items().stream().filter(i -> i.source().equals("DISCOVERY")).findFirst().orElseThrow().postId());
        var repaired = service.get(viewer, "home", first.nextCursor());
        assertThat(repaired.items()).hasSize(11);
        assertThat(repaired.items().stream().filter(i -> i.source().equals("FRIENDS"))).hasSize(5);
        assertThat(repaired.items().stream().filter(i -> i.source().equals("FOLLOWING"))).hasSize(2);
        assertThat(repaired.items().stream().filter(i -> i.source().equals("DISCOVERY"))).hasSize(4);
        assertThat(service.get(viewer, "home", first.nextCursor())).isEqualTo(repaired);
    }
}
