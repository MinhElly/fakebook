package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.*;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.criteria.PostCriteria;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.mapper.PostMapper;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import com.minh.fakebook.post.client.UserClient;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Service for executing complex queries for {@link Post} entities in the database.
 * The main input is a {@link PostCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PostDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PostQueryService extends QueryService<Post> {

    private static final Logger LOG = LoggerFactory.getLogger(PostQueryService.class);

    private final PostRepository postRepository;

    private final PostMapper postMapper;

    private final PostMediaRepository postMediaRepository;

    private final UserClient userClient;

    public PostQueryService(PostRepository postRepository, PostMapper postMapper,
            PostMediaRepository postMediaRepository,
            UserClient userClient) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.postMediaRepository = postMediaRepository;
        this.userClient = userClient;
    }

    /**
     * Return a {@link Page} of {@link PostDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PostDTO> findByCriteria(PostCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Post> specification = createSpecification(criteria);

        return postRepository.findAll(specification, page).map(post -> {
            PostDTO dto = postMapper.toDto(post);
            List<UUID> mediaIds = postMediaRepository
                    .findByPostIdOrderByDisplayOrderAsc(post.getId())
                    .stream()
                    .map(PostMedia::getMediaId)
                    .toList();
            dto.setMediaIds(mediaIds);
            return dto;
        });
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PostCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Post> specification = createSpecification(criteria);
        return postRepository.count(specification);
    }

    /**
         * Function to convert {@link PostCriteria} to a {@link Specification}
         * @param criteria The object which holds all the filters, which the entities should match.
         * @return the matching {@link Specification} of the entity.
         */
        protected Specification<Post> createSpecification(PostCriteria criteria) {
        Specification<Post> specification = Specification.unrestricted();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        List<UUID> fetchedFriendIds = new ArrayList<>();
        if (isLoggedIn) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
            if (!isAdmin && auth instanceof JwtAuthenticationToken jwtAuth) {
                try {
                    fetchedFriendIds = userClient
                            .getFriendIdsByUserId(UUID.fromString(jwtAuth.getToken().getSubject()));
                } catch (Exception e) {
                    LOG.error("Error fetching friend ids from userService", e);
                }
            }
        }
        final List<UUID> finalFriendIds = fetchedFriendIds;

        Specification<Post> securitySpec = (root, query, builder) -> {
            Predicate isActive = builder.equal(root.get(Post_.status), PostStatus.ACTIVE);
            if (!isLoggedIn) {
                return builder.and(isActive, builder.equal(root.get(Post_.visibility),
                        PostVisibility.PUBLIC));
            }
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
            if (isAdmin) {
                return isActive;
            }
            UUID currentUserId = UUID.fromString(((JwtAuthenticationToken) auth).getToken().getSubject());
            Predicate isPublic = builder.equal(root.get(Post_.visibility), PostVisibility.PUBLIC);
            Predicate isOwner = builder.equal(root.get(Post_.authorId), currentUserId);

            Predicate isFriendsCondition = finalFriendIds.isEmpty() ? builder.disjunction()
                    : root.get(Post_.authorId).in(finalFriendIds);
            Predicate isFriends = builder.and(
                    builder.equal(root.get(Post_.visibility), PostVisibility.FRIENDS),
                    isFriendsCondition);
            return builder.and(isActive, builder.or(isPublic, isOwner, isFriends));
        };

        specification = specification.and(securitySpec);

        if (criteria != null) {
            specification = specification.and(Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct())
                            : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Post_.id),
                    buildSpecification(criteria.getAuthorId(), Post_.authorId),
                    buildSpecification(criteria.getVisibility(), Post_.visibility),
                    buildSpecification(criteria.getStatus(), Post_.status),
                    buildRangeSpecification(criteria.getCreatedAt(), Post_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Post_.updatedAt)));
        }
        return specification;
    }
}
