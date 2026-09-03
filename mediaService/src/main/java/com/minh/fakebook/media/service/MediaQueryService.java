package com.minh.fakebook.media.service;

import com.minh.fakebook.media.domain.*; // for static metamodels
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.criteria.MediaCriteria;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Media} entities in the database.
 * The main input is a {@link MediaCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MediaDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MediaQueryService extends QueryService<Media> {

    private static final Logger LOG = LoggerFactory.getLogger(MediaQueryService.class);

    private final MediaRepository mediaRepository;

    private final MediaMapper mediaMapper;

    public MediaQueryService(MediaRepository mediaRepository, MediaMapper mediaMapper) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
    }

    /**
     * Return a {@link Page} of {@link MediaDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MediaDTO> findByCriteria(MediaCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Media> specification = createSpecification(criteria);
        return mediaRepository.findAll(specification, page).map(mediaMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MediaCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Media> specification = createSpecification(criteria);
        return mediaRepository.count(specification);
    }

    /**
     * Function to convert {@link MediaCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Media> createSpecification(MediaCriteria criteria) {
        Specification<Media> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Media_.id),
                    buildSpecification(criteria.getOwnerId(), Media_.ownerId),
                    buildStringSpecification(criteria.getFileName(), Media_.fileName),
                    buildSpecification(criteria.getMediaType(), Media_.mediaType),
                    buildStringSpecification(criteria.getMimeType(), Media_.mimeType),
                    buildRangeSpecification(criteria.getFileSize(), Media_.fileSize),
                    buildSpecification(criteria.getStorageProvider(), Media_.storageProvider),
                    buildStringSpecification(criteria.getStorageKey(), Media_.storageKey),
                    buildStringSpecification(criteria.getUrl(), Media_.url),
                    buildSpecification(criteria.getStatus(), Media_.status),
                    buildRangeSpecification(criteria.getCreatedAt(), Media_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Media_.updatedAt)
                )
            );
        }
        return specification;
    }
}
