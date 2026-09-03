package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.*; // for static metamodels
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.service.criteria.PostMediaCriteria;
import com.minh.fakebook.post.service.dto.PostMediaDTO;
import com.minh.fakebook.post.service.mapper.PostMediaMapper;
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
 * Service for executing complex queries for {@link PostMedia} entities in the database.
 * The main input is a {@link PostMediaCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PostMediaDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PostMediaQueryService extends QueryService<PostMedia> {

    private static final Logger LOG = LoggerFactory.getLogger(PostMediaQueryService.class);

    private final PostMediaRepository postMediaRepository;

    private final PostMediaMapper postMediaMapper;

    public PostMediaQueryService(PostMediaRepository postMediaRepository, PostMediaMapper postMediaMapper) {
        this.postMediaRepository = postMediaRepository;
        this.postMediaMapper = postMediaMapper;
    }

    /**
     * Return a {@link Page} of {@link PostMediaDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PostMediaDTO> findByCriteria(PostMediaCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PostMedia> specification = createSpecification(criteria);
        return postMediaRepository.findAll(specification, page).map(postMediaMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PostMediaCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PostMedia> specification = createSpecification(criteria);
        return postMediaRepository.count(specification);
    }

    /**
     * Function to convert {@link PostMediaCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PostMedia> createSpecification(PostMediaCriteria criteria) {
        Specification<PostMedia> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(PostMedia_.post, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), PostMedia_.id),
                    buildSpecification(criteria.getMediaId(), PostMedia_.mediaId),
                    buildRangeSpecification(criteria.getDisplayOrder(), PostMedia_.displayOrder),
                    buildRangeSpecification(criteria.getCreatedAt(), PostMedia_.createdAt),
                    buildSpecification(criteria.getPostId(), root -> root.join(PostMedia_.post, JoinType.LEFT).get(Post_.id))
                )
            );
        }
        return specification;
    }
}
