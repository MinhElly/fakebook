package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.*; // for static metamodels
import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.service.criteria.FriendRequestCriteria;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
import com.minh.fakebook.user.service.mapper.FriendRequestMapper;
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
 * Service for executing complex queries for {@link FriendRequest} entities in the database.
 * The main input is a {@link FriendRequestCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link FriendRequestDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class FriendRequestQueryService extends QueryService<FriendRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestQueryService.class);

    private final FriendRequestRepository friendRequestRepository;

    private final FriendRequestMapper friendRequestMapper;

    public FriendRequestQueryService(FriendRequestRepository friendRequestRepository, FriendRequestMapper friendRequestMapper) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendRequestMapper = friendRequestMapper;
    }

    /**
     * Return a {@link Page} of {@link FriendRequestDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<FriendRequestDTO> findByCriteria(FriendRequestCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<FriendRequest> specification = createSpecification(criteria);
        return friendRequestRepository.findAll(specification, page).map(friendRequestMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(FriendRequestCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<FriendRequest> specification = createSpecification(criteria);
        return friendRequestRepository.count(specification);
    }

    /**
     * Function to convert {@link FriendRequestCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<FriendRequest> createSpecification(FriendRequestCriteria criteria) {
        Specification<FriendRequest> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(FriendRequest_.sender, JoinType.LEFT);
                root.fetch(FriendRequest_.receiver, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), FriendRequest_.id),
                    buildSpecification(criteria.getStatus(), FriendRequest_.status),
                    buildRangeSpecification(criteria.getCreatedAt(), FriendRequest_.createdAt),
                    buildRangeSpecification(criteria.getRespondedAt(), FriendRequest_.respondedAt),
                    buildSpecification(criteria.getSenderId(), root ->
                        root.join(FriendRequest_.sender, JoinType.LEFT).get(UserProfile_.id)
                    ),
                    buildSpecification(criteria.getReceiverId(), root ->
                        root.join(FriendRequest_.receiver, JoinType.LEFT).get(UserProfile_.id)
                    )
                )
            );
        }
        return specification;
    }
}
