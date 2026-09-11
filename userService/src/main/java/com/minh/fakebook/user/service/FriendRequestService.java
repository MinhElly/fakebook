package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
import com.minh.fakebook.user.service.mapper.FriendRequestMapper;


import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.annotations.Cache;
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
 * Service Implementation for managing
 * {@link com.minh.fakebook.user.domain.FriendRequest}.
 */
@Service
@Transactional
public class FriendRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestService.class);

    private final FriendRequestRepository friendRequestRepository;

    private final FriendshipRepository friendshipRepository;

    private final FriendRequestMapper friendRequestMapper;

    private final UserProfileRepository userProfileRepository;

    public FriendRequestService(
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestMapper friendRequestMapper,
            UserProfileRepository userProfileRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestMapper = friendRequestMapper;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Save a friendRequest.
     *
     * @param friendRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public FriendRequestDTO save(FriendRequestDTO friendRequestDTO) {
        LOG.debug("Request to save FriendRequest : {}", friendRequestDTO);

        if (friendRequestDTO.getSender() != null && friendRequestDTO.getReceiver() != null) {
            UUID senderId = friendRequestDTO.getSender().getId();
            UUID receiverId = friendRequestDTO.getReceiver().getId();

            if (senderId != null && receiverId != null) {
                if (friendshipRepository.existsFriendship(senderId, receiverId)) {
                    throw new IllegalArgumentException("Hai người dùng đã là bạn bè của nhau");
                }
                if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId,
                        FriendRequestStatus.PENDING) ||
                        friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId,
                                FriendRequestStatus.PENDING)) {
                    throw new IllegalArgumentException("Lời mời kết bạn đã tồn tại và đang chờ phản hồi");
                }
            }
        }

        FriendRequest friendRequest = friendRequestMapper.toEntity(friendRequestDTO);
        friendRequest = friendRequestRepository.save(friendRequest);
        return friendRequestMapper.toDto(friendRequest);
    }

    /**
     * Update a friendRequest.
     *
     * @param friendRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public FriendRequestDTO update(FriendRequestDTO friendRequestDTO) {
        LOG.debug("Request to update FriendRequest : {}", friendRequestDTO);
        FriendRequest friendRequest = friendRequestMapper.toEntity(friendRequestDTO);
        friendRequest = friendRequestRepository.save(friendRequest);
        return friendRequestMapper.toDto(friendRequest);
    }

    /**
     * Partially update a friendRequest.
     *
     * @param friendRequestDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FriendRequestDTO> partialUpdate(FriendRequestDTO friendRequestDTO) {
        LOG.debug("Request to partially update FriendRequest : {}", friendRequestDTO);

        return friendRequestRepository
                .findById(friendRequestDTO.getId())
                .map(existingFriendRequest -> {
                    friendRequestMapper.partialUpdate(existingFriendRequest, friendRequestDTO);

                    return existingFriendRequest;
                })
                .map(friendRequestRepository::save)
                .map(friendRequestMapper::toDto);
    }

    /**
     * Get one friendRequest by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FriendRequestDTO> findOne(UUID id) {
        LOG.debug("Request to get FriendRequest : {}", id);
        return friendRequestRepository.findById(id).map(friendRequestMapper::toDto);
    }

    /**
     * Delete the friendRequest by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete FriendRequest : {}", id);
        friendRequestRepository.deleteById(id);
    }
    @Transactional 
    @Caching (evict = {
        @CacheEvict(value = "pendingSentRequests", allEntries = true),
        @CacheEvict(value = "pendingReceivedRequests", allEntries = true),
        @CacheEvict(value = "friendSuggestions", allEntries = true)
    })
    public FriendRequestDTO sendFriendRequest (UUID senderId, UUID targetUserId){
        if(senderId.equals(targetUserId)){
            throw new IllegalArgumentException("Sender and target user must be different people");
        }
        UserProfile sender = userProfileRepository.findById(senderId).orElseThrow(() -> new EntityNotFoundException("Sender "+ senderId +" not found"));
        UserProfile receiver = userProfileRepository.findById(targetUserId).orElseThrow(() -> new EntityNotFoundException("Receiver "+ targetUserId +" not found"));

        if(friendshipRepository.existsFriendship(senderId, targetUserId)){
            throw new IllegalStateException("Users are alrealy friends");
        }
        boolean requestExists = 
        friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, targetUserId, FriendRequestStatus.PENDING) ||
        friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(targetUserId, senderId, FriendRequestStatus.PENDING);
        if(requestExists){
            throw new IllegalStateException("Friend request already exists");
        }
        FriendRequest friendRequest = new FriendRequest();
        
        friendRequest.sender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequest.setCreatedAt(Instant.now());
        friendRequest = friendRequestRepository.save(friendRequest);
        return friendRequestMapper.toDto(friendRequest);  
    }
    @Transactional
    @Caching(evict = {
        @CacheEvict (value = "pendingReceivedRequests", allEntries = true),
        @CacheEvict (value = "pendingSentRequests", allEntries = true),
        @CacheEvict (value = "userFriends", allEntries = true),
        @CacheEvict (value = "friendSuggestions", allEntries = true)
    }) 
    public FriendRequestDTO acceptFriendRequest(UUID requestId, UUID currentId){
        FriendRequest friendRequest = friendRequestRepository.findById(requestId).orElseThrow(() -> new EntityNotFoundException("Friend request not found"));
        if(friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Friend request is not in pending status");
        }
        UserProfile receiver = friendRequest.getReceiver();
        if (!currentId.equals(receiver.getId())) {
            throw new IllegalArgumentException("You are not the receiver of this friend request");
        }
        UserProfile sender = friendRequest.getSender();
        if(friendshipRepository.existsFriendship(sender.getId(), receiver.getId())) {
            throw new IllegalStateException("Users are already friends");
        }
        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.respondedAt(Instant.now());

        Friendship senderToReceiver = new Friendship();
        senderToReceiver.setUser(sender);
        senderToReceiver.setFriend(receiver);
        senderToReceiver.createdAt(Instant.now());

        Friendship receiverToSender = new Friendship();
        receiverToSender.setUser(receiver);
        receiverToSender.setFriend(sender);
        receiverToSender.createdAt(Instant.now());

        friendshipRepository.save(senderToReceiver);
        friendshipRepository.save(receiverToSender);

        FriendRequest saved = friendRequestRepository.save(friendRequest);

        return friendRequestMapper.toDto(saved);

    }
    @Transactional
    @Caching(evict = {
        @CacheEvict (value = "pendingReceivedRequests", allEntries = true),
        @CacheEvict (value = "friendSuggestions", allEntries = true)
    })
    public FriendRequestDTO rejectFriendRequest(UUID requestId, UUID currentId){
        FriendRequest friendRequest = friendRequestRepository.findById(requestId).orElseThrow(() -> new EntityNotFoundException("Friend request not found"));
        if(friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Friend request is not in pending status");
        }
        UserProfile receiver = friendRequest.getReceiver();
        if (!currentId.equals(receiver.getId())) {
            throw new IllegalArgumentException("You are not the receiver of this friend request");
        }
        UserProfile sender = friendRequest.getSender();
        if(friendshipRepository.existsFriendship(sender.getId(), receiver.getId())) {
            throw new IllegalStateException("Users are already friends");
        }
        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.respondedAt(Instant.now());

        FriendRequest saved = friendRequestRepository.save(friendRequest);

        return friendRequestMapper.toDto(saved);
    }
    @Transactional
    @Caching(evict = {
        @CacheEvict (value = "pendingSentRequests", allEntries = true),
        @CacheEvict (value = "friendSuggestions", allEntries = true)
    })
    public FriendRequestDTO cancelFriendRequest(UUID requestId, UUID currentId){
        FriendRequest friendRequest = friendRequestRepository.findById(requestId).orElseThrow(() -> new EntityNotFoundException("Friend request not found"));
        if(friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Friend request is not in pending status");
        }
        UserProfile sender = friendRequest.getSender();
        if (!currentId.equals(sender.getId())) {
            throw new IllegalArgumentException("You are not the sender of this friend request");
        }
        UserProfile receiver = friendRequest.getReceiver();
        if(friendshipRepository.existsFriendship(sender.getId(), receiver.getId())) {
            throw new IllegalStateException("Users are already friends");
        }
        friendRequest.setStatus(FriendRequestStatus.CANCELLED);
        friendRequest.respondedAt(Instant.now());

        FriendRequest saved = friendRequestRepository.save(friendRequest);

        return friendRequestMapper.toDto(saved);
    }
    @Cacheable (value = "pendingReceivedRequests", key = "#receiverId.toString() + '_' + #pageable.getPageNumber() + '_' + #pageable.getPageSize()")
    public Page<FriendRequestDTO> getReceivedPendingRequests(UUID receiverId, Pageable pageable) {
        return friendRequestRepository.findByReceiverIdAndStatus(receiverId, FriendRequestStatus.PENDING, pageable)
                .map(friendRequestMapper::toDto);
    }
    @Cacheable (value = "pendingSentRequests", key = "#senderId.toString() + '_' + #pageable.getPageNumber() + '_' + #pageable.getPageSize()")
    public Page<FriendRequestDTO> getSentPendingRequests(UUID senderId, Pageable pageable) {
        return friendRequestRepository.findBySenderIdAndStatus(senderId, FriendRequestStatus.PENDING, pageable)
                .map(friendRequestMapper::toDto);
    }


}
 