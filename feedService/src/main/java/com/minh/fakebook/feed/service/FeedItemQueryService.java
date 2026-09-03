package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.domain.*; // for static metamodels
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.criteria.FeedItemCriteria;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link FeedItem} entities in the database.
 * The main input is a {@link FeedItemCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link FeedItemDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class FeedItemQueryService extends QueryService<FeedItem> {

    private static final Logger LOG = LoggerFactory.getLogger(FeedItemQueryService.class);

    private final FeedItemRepository feedItemRepository;

    private final FeedItemMapper feedItemMapper;

    public FeedItemQueryService(FeedItemRepository feedItemRepository, FeedItemMapper feedItemMapper) {
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
    }

    /**
     * Return a {@link Page} of {@link FeedItemDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<FeedItemDTO> findByCriteria(FeedItemCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<FeedItem> specification = createSpecification(criteria);
        return feedItemRepository.findAll(specification, page).map(feedItemMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(FeedItemCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<FeedItem> specification = createSpecification(criteria);
        return feedItemRepository.count(specification);
    }

    /**
     * Function to convert {@link FeedItemCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<FeedItem> createSpecification(FeedItemCriteria criteria) {
        Specification<FeedItem> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), FeedItem_.id),
                    buildSpecification(criteria.getUserId(), FeedItem_.userId),
                    buildSpecification(criteria.getPostId(), FeedItem_.postId),
                    buildRangeSpecification(criteria.getCreatedAt(), FeedItem_.createdAt)
                )
            );
        }
        return specification;
    }
}
