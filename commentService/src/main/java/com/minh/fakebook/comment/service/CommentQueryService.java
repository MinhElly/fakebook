package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.*; // for static metamodels
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import com.minh.fakebook.comment.security.SecurityUtils;
import com.minh.fakebook.comment.service.criteria.CommentCriteria;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.mapper.CommentMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import java.util.List;

/**
 * Service for executing complex queries for {@link Comment} entities in the database.
 * The main input is a {@link CommentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CommentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CommentQueryService extends QueryService<Comment> {

    private static final Logger LOG = LoggerFactory.getLogger(CommentQueryService.class);

    private final CommentRepository commentRepository;

    private final CommentMapper commentMapper;

    private final CommentViewAuthorizationService authorizationService;

    public CommentQueryService(
        CommentRepository commentRepository,
        CommentMapper commentMapper,
        CommentViewAuthorizationService authorizationService
    ) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.authorizationService = authorizationService;
    }

    /**
     * Return a {@link Page} of {@link CommentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CommentDTO> findByCriteria(CommentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        verifyViewPermission(criteria);
        final Specification<Comment> specification = createSpecification(criteria);
        return commentRepository.findAll(specification, page).map(commentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CommentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        verifyViewPermission(criteria);
        final Specification<Comment> specification = createSpecification(criteria);
        return commentRepository.count(specification);
    }

    /**
     * Function to convert {@link CommentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Comment> createSpecification(CommentCriteria criteria) {
        Specification<Comment> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Comment_.parentComment, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Comment_.id),
                    buildSpecification(criteria.getPostId(), Comment_.postId),
                    buildSpecification(criteria.getAuthorId(), Comment_.authorId),
                    buildSpecification(criteria.getStatus(), Comment_.status),
                    buildSpecification(criteria.getParentCommentId(), root ->
                        root.join(Comment_.parentComment, JoinType.LEFT).get(Comment_.id)
                    )
                )
            );
        }
        return specification;
    }

    private void verifyViewPermission(CommentCriteria criteria) {
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            return;
        }
        if (criteria == null || criteria.getPostId() == null || criteria.getPostId().getEquals()
            == null) {
            throw new org.springframework.security.access.AccessDeniedException("User must provide a specific postId to view comments.");
        }
        authorizationService.verifyCanView(List.of(criteria.getPostId().getEquals()));
    }
}
