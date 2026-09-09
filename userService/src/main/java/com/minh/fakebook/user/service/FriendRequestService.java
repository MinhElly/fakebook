package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
import com.minh.fakebook.user.service.mapper.FriendRequestMapper;
import com.minh.fakebook.user.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.user.domain.FriendRequest}.
 */
@Service
@Transactional
public class FriendRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestService.class);

    private final FriendRequestRepository friendRequestRepository;

    private final FriendshipRepository friendshipRepository;

    private final FriendRequestMapper friendRequestMapper;

    public FriendRequestService(
        FriendRequestRepository friendRequestRepository,
        FriendshipRepository friendshipRepository,
        FriendRequestMapper friendRequestMapper
    ) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestMapper = friendRequestMapper;
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
                    throw new BadRequestAlertException("Hai người dùng đã là bạn bè của nhau", "userServiceFriendRequest", "alreadyfriends");
                }
                if (
                    friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, FriendRequestStatus.PENDING) ||
                    friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, FriendRequestStatus.PENDING)
                ) {
                    throw new BadRequestAlertException("Lời mời kết bạn đã tồn tại và đang chờ phản hồi", "userServiceFriendRequest", "alreadyrequested");
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
}
