package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.*;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.criteria.PostReactionCriteria;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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

/**
 * Service for executing complex queries for {@link PostReaction} entities in the database.
 * The main input is a {@link PostReactionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PostReactionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PostReactionQueryService extends QueryService<PostReaction> {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionQueryService.class);

    private final PostReactionRepository postReactionRepository;

    private final PostReactionMapper postReactionMapper;

    public PostReactionQueryService(PostReactionRepository postReactionRepository, PostReactionMapper postReactionMapper) {
        this.postReactionRepository = postReactionRepository;
        this.postReactionMapper = postReactionMapper;
    }

    /**
     * Return a {@link Page} of {@link PostReactionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PostReactionDTO> findByCriteria(PostReactionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PostReaction> specification = createSpecification(criteria);
        return postReactionRepository.findAll(specification, page).map(postReactionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PostReactionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PostReaction> specification = createSpecification(criteria);
        return postReactionRepository.count(specification);
    }

    /**
     * Function to convert {@link PostReactionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PostReaction> createSpecification(PostReactionCriteria criteria) {
        Specification<PostReaction> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(PostReaction_.post, JoinType.LEFT);
            }
            return null;
        });
        specification = specification.and((root, query, builder) -> {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            boolean isGuest = (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()));
            boolean isAdmin = !isGuest && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));

            if (isAdmin) {
                return builder.conjunction();
            }

            // JOIN PostReaction with Post to filter based on Post's visibility and status
            Join<PostReaction, Post> postJoin = root
                    .join(PostReaction_.post, JoinType.INNER);

            Predicate isActive = builder.equal(postJoin.get(Post_.status),
                    PostStatus.ACTIVE);

            if (isGuest) {
                Predicate isPublic = builder.equal(postJoin.get(Post_.visibility),
                        PostVisibility.PUBLIC);
                return builder.and(isActive, isPublic);
            }

            String sub = ((JwtAuthenticationToken) auth)
                    .getToken().getSubject();
            UUID currentUserId = UUID.fromString(sub);

            Predicate isPublic = builder.equal(postJoin.get(Post_.visibility),
                    PostVisibility.PUBLIC);
            Predicate isFriends = builder.equal(postJoin.get(Post_.visibility),
                    PostVisibility.FRIENDS);
            Predicate isAuthor = builder.equal(postJoin.get(Post_.authorId),
                    currentUserId);

            return builder.and(isActive, builder.or(isPublic, isFriends, isAuthor));
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), PostReaction_.id),
                    buildSpecification(criteria.getUserId(), PostReaction_.userId),
                    buildSpecification(criteria.getReactionType(), PostReaction_.reactionType),
                    buildRangeSpecification(criteria.getCreatedAt(), PostReaction_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), PostReaction_.updatedAt),
                    buildSpecification(criteria.getPostId(), root -> root.join(PostReaction_.post, JoinType.LEFT).get(Post_.id))
                )
            );
        }
        return specification;
    }
}
