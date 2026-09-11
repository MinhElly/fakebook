package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.Follow;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.FollowRepository;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.FollowDTO;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.service.mapper.FollowMapper;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.user.domain.Follow}.
 */
@Service
@Transactional
public class FollowService {

    private static final Logger LOG = LoggerFactory.getLogger(FollowService.class);

    private final FollowRepository followRepository;

    private final FollowMapper followMapper;

    private final UserProfileRepository userProfileRepository;

    public FollowService(FollowRepository followRepository, FollowMapper followMapper, UserProfileRepository userProfileRepository) {
        this.followRepository = followRepository;
        this.followMapper = followMapper;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Save a follow.
     *
     * @param followDTO the entity to save.
     * @return the persisted entity.
     */
    public FollowDTO save(FollowDTO followDTO) {
        LOG.debug("Request to save Follow : {}", followDTO);
        Follow follow = followMapper.toEntity(followDTO);
        follow = followRepository.save(follow);
        return followMapper.toDto(follow);
    }

    /**
     * Update a follow.
     *
     * @param followDTO the entity to save.
     * @return the persisted entity.
     */
    public FollowDTO update(FollowDTO followDTO) {
        LOG.debug("Request to update Follow : {}", followDTO);
        Follow follow = followMapper.toEntity(followDTO);
        follow = followRepository.save(follow);
        return followMapper.toDto(follow);
    }

    /**
     * Partially update a follow.
     *
     * @param followDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FollowDTO> partialUpdate(FollowDTO followDTO) {
        LOG.debug("Request to partially update Follow : {}", followDTO);

        return followRepository
            .findById(followDTO.getId())
            .map(existingFollow -> {
                followMapper.partialUpdate(existingFollow, followDTO);

                return existingFollow;
            })
            .map(followRepository::save)
            .map(followMapper::toDto);
    }

    /**
     * Get one follow by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FollowDTO> findOne(UUID id) {
        LOG.debug("Request to get Follow : {}", id);
        return followRepository.findById(id).map(followMapper::toDto);
    }

    /**
     * Delete the follow by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Follow : {}", id);
        followRepository.deleteById(id);
    }

    @Caching(evict = {
        @CacheEvict(value = "userFollowing", allEntries = true),
        @CacheEvict(value = "userFollowers", allEntries = true),
        @CacheEvict(value = "friendSuggestions", allEntries = true)
    })
    public FollowDTO followUser(UUID senderId, UUID targetUserId){
        if(senderId.equals(targetUserId)){
            throw new IllegalArgumentException("Sender and targer user must be different people");
        }
        UserProfile sender = userProfileRepository.findById(senderId).orElseThrow(() -> new EntityNotFoundException("Sender "+ senderId +" not found"));
        UserProfile receiver = userProfileRepository.findById(targetUserId).orElseThrow(() -> new EntityNotFoundException("Receiver "+ targetUserId +" not found"));

        if(followRepository.existsFollowing(senderId, targetUserId)){
            throw new IllegalStateException("You're already follow this user");
        }
        Follow follow = new Follow();
        follow.setFollower(sender);
        follow.setFollowing(receiver);
        follow.setCreatedAt(Instant.now());
        return followMapper.toDto(followRepository.save(follow));
    }

    @Caching(evict = {
        @CacheEvict(value = "userFollowing", allEntries = true),
        @CacheEvict(value = "userFollowers", allEntries = true),
        @CacheEvict(value = "friendSuggestions", allEntries = true)
    })
    public void unfollowUser(UUID currentUserId, UUID targetUserId){
    if(currentUserId.equals(targetUserId)){
       throw new IllegalArgumentException("Cannot unfollow yourself");
    }
    if(!followRepository.existsFollowing(currentUserId, targetUserId)){
        throw new IllegalStateException("You are not follow this user");
    }else{
        followRepository.deleteFollowing(currentUserId, targetUserId);
        }  
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userFollowing", key = "#userId.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<FollowDTO> getFollowingList(UUID userId, Pageable pageable){
        return followRepository.findFollowing(userId, pageable).map(followMapper::toDto);    
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userFollowers", key = "#userId.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<FollowDTO> getFollowerList(UUID userId, Pageable pageable){
        return followRepository.findFollowers(userId, pageable).map(followMapper::toDto);    
    }
}
