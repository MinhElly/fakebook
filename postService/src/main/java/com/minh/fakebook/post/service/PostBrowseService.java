package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.*;
import com.minh.fakebook.post.repository.*;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.mapper.PostMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostBrowseService {
    private final PostRepository posts;
    private final PostMediaRepository media;
    private final PostQueryService query;
    private final PostService service;
    private final PostMapper mapper;
    private final PostHistoryService history;
    private final PostContentCache postContentCache;

    public PostBrowseService(PostRepository posts, PostMediaRepository media, PostQueryService query,
            PostService service, PostMapper mapper, PostHistoryService history,
            PostContentCache postContentCache) {
        this.posts = posts; this.media = media; this.query = query;
        this.service = service; this.mapper = mapper; this.history = history;
        this.postContentCache = postContentCache;
    }

    public record ProfilePage(List<PostDTO> items, String nextCursor, boolean hasMore) {}
    public record Candidate(UUID postId, UUID authorId, String visibility, Instant createdAt) {}
    public record CandidateRequest(UUID viewerId, Set<UUID> friends, Set<UUID> following, String source,
        Instant asOf, String cursor, int limit) {}
    public record CandidatePage(List<Candidate> items, String nextCursor, boolean hasMore) {}

    public ProfilePage profile(UUID user, String cursor, int size) {
        Specification<Post> membership = (root, q, cb) -> {
            q.distinct(true);
            return cb.or(cb.equal(root.get("authorId"), user), cb.isMember(user, root.get("taggedUserIds")));
        };
        var list = page(query.createSpecification(null).and(membership), cursor, Math.clamp(size, 1, 50));
        size = Math.clamp(size, 1, 50);
        boolean more = list.size() > size;
        var selected = list.stream().limit(size).toList();
        return new ProfilePage(selected.stream().map(this::dto).toList(), more ? encode(selected.getLast()) : null, more);
    }

    public List<PostHistoryService.Revision> history(UUID id, Long before, int size) {
        service.findOne(id).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return history.list(id, before, size);
    }

    public boolean mediaReadable(UUID id) {
        for (var link : media.findByMediaId(id)) {
            if (link.getPost().getStatus() != PostStatus.ACTIVE) continue;
            try { if (service.findOne(link.getPost().getId()).isPresent()) return true; }
            catch (AccessDeniedException denied) { /* Another parent may grant access. */ }
        }
        return false;
    }

    public CandidatePage candidates(CandidateRequest r) {
        if (!Set.of("FRIENDS", "FOLLOWING", "DISCOVERY").contains(r.source()) || r.asOf() == null) throw new IllegalArgumentException("Invalid candidate request");
        Set<UUID> friends = r.friends() == null ? Set.of() : r.friends();
        Set<UUID> following = r.following() == null ? Set.of() : r.following();
        Set<UUID> connected = new HashSet<>(friends); connected.addAll(following);
        if (r.viewerId() != null) connected.add(r.viewerId());
        Specification<Post> spec = (root, q, cb) -> {
            var active = cb.and(cb.equal(root.get("status"), PostStatus.ACTIVE), cb.lessThanOrEqualTo(root.get("createdAt"), r.asOf()));
            var own = r.viewerId() == null ? cb.disjunction() : cb.equal(root.get("authorId"), r.viewerId());
            var friend = friends.isEmpty() ? cb.disjunction() : root.get("authorId").in(friends);
            var publicPost = cb.equal(root.get("visibility"), PostVisibility.PUBLIC);
            var canRead = cb.or(own, publicPost, cb.and(friend, cb.equal(root.get("visibility"), PostVisibility.FRIENDS)));
            var source = switch (r.source()) {
                case "FRIENDS" -> cb.or(own, friend);
                case "FOLLOWING" -> following.isEmpty() ? cb.disjunction() : cb.and(root.get("authorId").in(following), cb.not(cb.or(own, friend)), publicPost);
                default -> connected.isEmpty() ? publicPost : cb.and(publicPost, cb.not(root.get("authorId").in(connected)));
            };
            return cb.and(active, canRead, source);
        };
        int size = Math.clamp(r.limit(), 1, 100);
        var rows = page(spec, r.cursor(), size);
        boolean more = rows.size() > size;
        var selected = rows.stream().limit(size).toList();
        return new CandidatePage(selected.stream().map(p -> new Candidate(p.getId(), p.getAuthorId(), p.getVisibility().name(), p.getCreatedAt())).toList(),
            more ? encode(selected.getLast()) : null, more);
    }

    public List<Candidate> readable(List<UUID> ids, UUID viewer, Set<UUID> friends) {
        if (ids == null || ids.isEmpty()) return List.of();
        Map<UUID, com.minh.fakebook.post.service.dto.PostContentCacheDTO> postMap = postContentCache.getPosts(ids);
        return ids.stream()
            .map(postMap::get)
            .filter(Objects::nonNull)
            .filter(p -> !p.isEmpty())
            .filter(p -> p.status() == PostStatus.ACTIVE &&
                (p.visibility() == PostVisibility.PUBLIC || p.authorId().equals(viewer) ||
                 p.visibility() == PostVisibility.FRIENDS && friends != null && friends.contains(p.authorId())))
            .map(p -> new Candidate(p.id(), p.authorId(), p.visibility().name(), p.createdAt())).toList();
    }

    private List<Post> page(Specification<Post> spec, String cursor, int size) {
        if (cursor != null && !cursor.isBlank()) {
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", 2);
                Instant time = Instant.parse(parts[0]); UUID id = UUID.fromString(parts[1]);
                spec = spec.and((root, q, cb) -> cb.or(cb.lessThan(root.get("createdAt"), time),
                    cb.and(cb.equal(root.get("createdAt"), time), cb.lessThan(root.get("id"), id))));
            } catch (Exception invalid) { throw new IllegalArgumentException("Invalid profile/candidate cursor", invalid); }
        }
        return posts.findAll(spec, PageRequest.of(0, size + 1, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).getContent();
    }
    private static String encode(Post post) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((post.getCreatedAt() + "|" + post.getId()).getBytes(StandardCharsets.UTF_8));
    }
    private PostDTO dto(Post p) {
        PostDTO dto = mapper.toDto(p);
        dto.setMediaIds(media.findByPostIdOrderByDisplayOrderAsc(p.getId()).stream().map(link -> link.getMediaId()).toList());
        return dto;
    }
}
