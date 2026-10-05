package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.*; // for static metamodels
import com.minh.fakebook.comment.domain.CommentReaction;
import com.minh.fakebook.comment.repository.CommentReactionRepository;
import com.minh.fakebook.comment.service.criteria.CommentReactionCriteria;
import com.minh.fakebook.comment.service.dto.CommentReactionDTO;
import com.minh.fakebook.comment.service.mapper.CommentReactionMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link CommentReaction} entities in the database.
 * The main input is a {@link CommentReactionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CommentReactionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CommentReactionQueryService extends QueryService<CommentReaction> {

    private static final Logger LOG = LoggerFactory.getLogger(CommentReactionQueryService.class);

    private final CommentReactionRepository commentReactionRepository;

    private final CommentReactionMapper commentReactionMapper;

    public CommentReactionQueryService(CommentReactionRepository commentReactionRepository, CommentReactionMapper commentReactionMapper) {
        this.commentReactionRepository = commentReactionRepository;
        this.commentReactionMapper = commentReactionMapper;
    }

    /**
     * Return a {@link Page} of {@link CommentReactionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CommentReactionDTO> findByCriteria(CommentReactionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CommentReaction> specification = createSpecification(criteria);
        return commentReactionRepository.findAll(specification, page).map(commentReactionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CommentReactionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CommentReaction> specification = createSpecification(criteria);
        return commentReactionRepository.count(specification);
    }

    /**
     * Function to convert {@link CommentReactionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CommentReaction> createSpecification(CommentReactionCriteria criteria) {
        Specification<CommentReaction> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(CommentReaction_.comment, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), CommentReaction_.id),
                    buildSpecification(criteria.getUserId(), CommentReaction_.userId),
                    buildSpecification(criteria.getReactionType(), CommentReaction_.reactionType),
                    buildRangeSpecification(criteria.getCreatedAt(), CommentReaction_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), CommentReaction_.updatedAt),
                    buildSpecification(criteria.getCommentId(), root -> root.join(CommentReaction_.comment, JoinType.LEFT).get(Comment_.id))
                )
            );
        }
        return specification;
    }
}
