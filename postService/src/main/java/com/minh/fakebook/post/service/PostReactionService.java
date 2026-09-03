package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.post.domain.PostReaction}.
 */
@Service
@Transactional
public class PostReactionService {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionService.class);

    private final PostReactionRepository postReactionRepository;

    private final PostReactionMapper postReactionMapper;

    public PostReactionService(PostReactionRepository postReactionRepository, PostReactionMapper postReactionMapper) {
        this.postReactionRepository = postReactionRepository;
        this.postReactionMapper = postReactionMapper;
    }

    /**
     * Save a postReaction.
     *
     * @param postReactionDTO the entity to save.
     * @return the persisted entity.
     */
    public PostReactionDTO save(PostReactionDTO postReactionDTO) {
        LOG.debug("Request to save PostReaction : {}", postReactionDTO);
        PostReaction postReaction = postReactionMapper.toEntity(postReactionDTO);
        postReaction = postReactionRepository.save(postReaction);
        return postReactionMapper.toDto(postReaction);
    }

    /**
     * Update a postReaction.
     *
     * @param postReactionDTO the entity to save.
     * @return the persisted entity.
     */
    public PostReactionDTO update(PostReactionDTO postReactionDTO) {
        LOG.debug("Request to update PostReaction : {}", postReactionDTO);
        PostReaction postReaction = postReactionMapper.toEntity(postReactionDTO);
        postReaction = postReactionRepository.save(postReaction);
        return postReactionMapper.toDto(postReaction);
    }

    /**
     * Partially update a postReaction.
     *
     * @param postReactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PostReactionDTO> partialUpdate(PostReactionDTO postReactionDTO) {
        LOG.debug("Request to partially update PostReaction : {}", postReactionDTO);

        return postReactionRepository
            .findById(postReactionDTO.getId())
            .map(existingPostReaction -> {
                postReactionMapper.partialUpdate(existingPostReaction, postReactionDTO);

                return existingPostReaction;
            })
            .map(postReactionRepository::save)
            .map(postReactionMapper::toDto);
    }

    /**
     * Get one postReaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PostReactionDTO> findOne(UUID id) {
        LOG.debug("Request to get PostReaction : {}", id);
        return postReactionRepository.findById(id).map(postReactionMapper::toDto);
    }

    /**
     * Delete the postReaction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete PostReaction : {}", id);
        postReactionRepository.deleteById(id);
    }
}
