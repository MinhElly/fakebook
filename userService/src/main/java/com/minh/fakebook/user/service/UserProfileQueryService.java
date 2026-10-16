package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.*; // for static metamodels
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.criteria.UserProfileCriteria;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link UserProfile} entities in the database.
 * The main input is a {@link UserProfileCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link UserProfileDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class UserProfileQueryService extends QueryService<UserProfile> {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileQueryService.class);

    private final UserProfileRepository userProfileRepository;

    private final UserProfileMapper userProfileMapper;

    private final UserProfileCache userProfileCache;

    public UserProfileQueryService(
        UserProfileRepository userProfileRepository,
        UserProfileMapper userProfileMapper,
        UserProfileCache userProfileCache
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.userProfileCache = userProfileCache;
    }

    /**
     * Return a {@link Page} of {@link UserProfileDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<UserProfileDTO> findByCriteria(UserProfileCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        if (isSimpleIdInQuery(criteria)) {
            java.util.List<java.util.UUID> requestedIds = criteria.getId().getIn();
            java.util.Map<java.util.UUID, UserProfileDTO> profileMap = userProfileCache.getProfiles(requestedIds);
            java.util.List<UserProfileDTO> orderedProfiles = requestedIds.stream()
                .distinct()
                .map(profileMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

            if (page == null || page.isUnpaged()) {
                return new org.springframework.data.domain.PageImpl<>(
                    orderedProfiles,
                    page != null ? page : Pageable.unpaged(),
                    orderedProfiles.size()
                );
            }

            int start = (int) page.getOffset();
            int end = Math.min(start + page.getPageSize(), orderedProfiles.size());
            java.util.List<UserProfileDTO> pagedList = start >= orderedProfiles.size()
                ? java.util.List.of()
                : orderedProfiles.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(pagedList, page, orderedProfiles.size());
        }

        final Specification<UserProfile> specification = createSpecification(criteria);
        return userProfileRepository.findAll(specification, page).map(userProfileMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(UserProfileCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        if (isSimpleIdInQuery(criteria)) {
            java.util.List<java.util.UUID> requestedIds = criteria.getId().getIn();
            return userProfileCache.getProfiles(requestedIds).size();
        }
        final Specification<UserProfile> specification = createSpecification(criteria);
        return userProfileRepository.count(specification);
    }

    private boolean isSimpleIdInQuery(UserProfileCriteria criteria) {
        if (criteria == null || criteria.getId() == null || criteria.getId().getIn() == null || criteria.getId().getIn().isEmpty()) {
            return false;
        }
        return criteria.getId().getEquals() == null
            && criteria.getId().getNotEquals() == null
            && criteria.getId().getSpecified() == null
            && criteria.getId().getNotIn() == null
            && criteria.getUsername() == null
            && criteria.getDisplayName() == null
            && criteria.getAvatarMediaId() == null
            && criteria.getCoverMediaId() == null
            && criteria.getCreatedAt() == null
            && criteria.getUpdatedAt() == null;
    }

    /**
     * Function to convert {@link UserProfileCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<UserProfile> createSpecification(UserProfileCriteria criteria) {
        Specification<UserProfile> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), UserProfile_.id),
                    buildStringSpecification(criteria.getUsername(), UserProfile_.username),
                    buildStringSpecification(criteria.getDisplayName(), UserProfile_.displayName),
                    buildSpecification(criteria.getAvatarMediaId(), UserProfile_.avatarMediaId),
                    buildSpecification(criteria.getCoverMediaId(), UserProfile_.coverMediaId),
                    buildRangeSpecification(criteria.getCreatedAt(), UserProfile_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), UserProfile_.updatedAt)
                )
            );
        }
        return specification;
    }
}
