package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.service.mapper.FriendshipMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.user.domain.Friendship}.
 */
@Service
@Transactional
public class FriendshipService {

    private static final Logger LOG = LoggerFactory.getLogger(FriendshipService.class);

    private final FriendshipRepository friendshipRepository;

    private final FriendshipMapper friendshipMapper;

    public FriendshipService(FriendshipRepository friendshipRepository, FriendshipMapper friendshipMapper) {
        this.friendshipRepository = friendshipRepository;
        this.friendshipMapper = friendshipMapper;
    }

    /**
     * Save a friendship.
     *
     * @param friendshipDTO the entity to save.
     * @return the persisted entity.
     */
    public FriendshipDTO save(FriendshipDTO friendshipDTO) {
        LOG.debug("Request to save Friendship : {}", friendshipDTO);
        Friendship friendship = friendshipMapper.toEntity(friendshipDTO);
        friendship = friendshipRepository.save(friendship);
        return friendshipMapper.toDto(friendship);
    }

    /**
     * Update a friendship.
     *
     * @param friendshipDTO the entity to save.
     * @return the persisted entity.
     */
    public FriendshipDTO update(FriendshipDTO friendshipDTO) {
        LOG.debug("Request to update Friendship : {}", friendshipDTO);
        Friendship friendship = friendshipMapper.toEntity(friendshipDTO);
        friendship = friendshipRepository.save(friendship);
        return friendshipMapper.toDto(friendship);
    }

    /**
     * Partially update a friendship.
     *
     * @param friendshipDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FriendshipDTO> partialUpdate(FriendshipDTO friendshipDTO) {
        LOG.debug("Request to partially update Friendship : {}", friendshipDTO);

        return friendshipRepository
            .findById(friendshipDTO.getId())
            .map(existingFriendship -> {
                friendshipMapper.partialUpdate(existingFriendship, friendshipDTO);

                return existingFriendship;
            })
            .map(friendshipRepository::save)
            .map(friendshipMapper::toDto);
    }

    /**
     * Get one friendship by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FriendshipDTO> findOne(UUID id) {
        LOG.debug("Request to get Friendship : {}", id);
        return friendshipRepository.findById(id).map(friendshipMapper::toDto);
    }

    /**
     * Delete the friendship by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Friendship : {}", id);
        friendshipRepository.deleteById(id);
    }
}
