package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.service.dto.PostMediaDTO;
import com.minh.fakebook.post.service.mapper.PostMediaMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.post.domain.PostMedia}.
 */
@Service
@Transactional
public class PostMediaService {

    private static final Logger LOG = LoggerFactory.getLogger(PostMediaService.class);

    private final PostMediaRepository postMediaRepository;

    private final PostMediaMapper postMediaMapper;

    public PostMediaService(PostMediaRepository postMediaRepository, PostMediaMapper postMediaMapper) {
        this.postMediaRepository = postMediaRepository;
        this.postMediaMapper = postMediaMapper;
    }

    /**
     * Save a postMedia.
     *
     * @param postMediaDTO the entity to save.
     * @return the persisted entity.
     */
    public PostMediaDTO save(PostMediaDTO postMediaDTO) {
        LOG.debug("Request to save PostMedia : {}", postMediaDTO);
        PostMedia postMedia = postMediaMapper.toEntity(postMediaDTO);
        postMedia = postMediaRepository.save(postMedia);
        return postMediaMapper.toDto(postMedia);
    }

    /**
     * Update a postMedia.
     *
     * @param postMediaDTO the entity to save.
     * @return the persisted entity.
     */
    public PostMediaDTO update(PostMediaDTO postMediaDTO) {
        LOG.debug("Request to update PostMedia : {}", postMediaDTO);
        PostMedia postMedia = postMediaMapper.toEntity(postMediaDTO);
        postMedia = postMediaRepository.save(postMedia);
        return postMediaMapper.toDto(postMedia);
    }

    /**
     * Partially update a postMedia.
     *
     * @param postMediaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PostMediaDTO> partialUpdate(PostMediaDTO postMediaDTO) {
        LOG.debug("Request to partially update PostMedia : {}", postMediaDTO);

        return postMediaRepository
            .findById(postMediaDTO.getId())
            .map(existingPostMedia -> {
                postMediaMapper.partialUpdate(existingPostMedia, postMediaDTO);

                return existingPostMedia;
            })
            .map(postMediaRepository::save)
            .map(postMediaMapper::toDto);
    }

    /**
     * Get one postMedia by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PostMediaDTO> findOne(UUID id) {
        LOG.debug("Request to get PostMedia : {}", id);
        return postMediaRepository.findById(id).map(postMediaMapper::toDto);
    }

    /**
     * Delete the postMedia by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete PostMedia : {}", id);
        postMediaRepository.deleteById(id);
    }
}
