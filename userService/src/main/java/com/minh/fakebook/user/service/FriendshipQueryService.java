package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.*; // for static metamodels
import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.service.criteria.FriendshipCriteria;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.service.mapper.FriendshipMapper;
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
 * Service for executing complex queries for {@link Friendship} entities in the database.
 * The main input is a {@link FriendshipCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link FriendshipDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class FriendshipQueryService extends QueryService<Friendship> {

    private static final Logger LOG = LoggerFactory.getLogger(FriendshipQueryService.class);

    private final FriendshipRepository friendshipRepository;

    private final FriendshipMapper friendshipMapper;

    public FriendshipQueryService(FriendshipRepository friendshipRepository, FriendshipMapper friendshipMapper) {
        this.friendshipRepository = friendshipRepository;
        this.friendshipMapper = friendshipMapper;
    }

    /**
     * Return a {@link Page} of {@link FriendshipDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<FriendshipDTO> findByCriteria(FriendshipCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Friendship> specification = createSpecification(criteria);
        return friendshipRepository.findAll(specification, page).map(friendshipMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(FriendshipCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Friendship> specification = createSpecification(criteria);
        return friendshipRepository.count(specification);
    }

    /**
     * Function to convert {@link FriendshipCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Friendship> createSpecification(FriendshipCriteria criteria) {
        Specification<Friendship> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Friendship_.user, JoinType.LEFT);
                root.fetch(Friendship_.friend, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Friendship_.id),
                    buildRangeSpecification(criteria.getCreatedAt(), Friendship_.createdAt),
                    buildSpecification(criteria.getUserId(), root -> root.join(Friendship_.user, JoinType.LEFT).get(UserProfile_.id)),
                    buildSpecification(criteria.getFriendId(), root -> root.join(Friendship_.friend, JoinType.LEFT).get(UserProfile_.id))
                )
            );
        }
        return specification;
    }
}
