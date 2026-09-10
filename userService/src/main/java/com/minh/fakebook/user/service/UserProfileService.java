package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.FriendSuggestionProjection;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.dto.UserProfileDetailDTO;
import com.minh.fakebook.user.service.dto.UserSearchDTO;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing
 * {@link com.minh.fakebook.user.domain.UserProfile}.
 */
@Service
@Transactional
public class UserProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    private final UserProfileMapper userProfileMapper;

    private final FriendshipRepository friendshipRepository;

    private final FriendRequestRepository friendRequestRepository;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            UserProfileMapper userProfileMapper,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    /**
     * Save a userProfile.
     *
     * @param userProfileDTO the entity to save.
     * @return the persisted entity.
     */
    @CacheEvict(value = "userProfile", key = "#userProfileDTO.id.toString()", condition = "#userProfileDTO.id != null")
    public UserProfileDTO save(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to save UserProfile : {}", userProfileDTO);
        UserProfile userProfile = userProfileMapper.toEntity(userProfileDTO);
        userProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toDto(userProfile);
    }

    /**
     * Update a userProfile.
     *
     * @param userProfileDTO the entity to save.
     * @return the persisted entity.
     */
    @CacheEvict(value = "userProfile", key = "#userProfileDTO.id.toString()")
    public UserProfileDTO update(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to update UserProfile : {}", userProfileDTO);
        UserProfile userProfile = userProfileMapper.toEntity(userProfileDTO);
        userProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toDto(userProfile);
    }

    /**
     * Partially update a userProfile.
     *
     * @param userProfileDTO the entity to update partially.
     * @return the persisted entity.
     */
    @CacheEvict(value = "userProfile", key = "#userProfileDTO.id.toString()", condition = "#result.isPresent()")
    public Optional<UserProfileDTO> partialUpdate(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to partially update UserProfile : {}", userProfileDTO);

        return userProfileRepository
                .findById(userProfileDTO.getId())
                .map(existingUserProfile -> {
                    userProfileMapper.partialUpdate(existingUserProfile, userProfileDTO);

                    return existingUserProfile;
                })
                .map(userProfileRepository::save)
                .map(userProfileMapper::toDto);
    }

    /**
     * Get one userProfile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userProfile", key = "#id.toString()")
    public Optional<UserProfileDTO> findOne(UUID id) {
        LOG.debug("Request to get UserProfile : {}", id);
        return userProfileRepository.findById(id).map(userProfileMapper::toDto);
    }

    /**
     * Delete the userProfile by id.
     *
     * @param id the id of the entity.
     */
    @CacheEvict(value = "userProfile", key = "#id.toString()")
    public void delete(UUID id) {
        LOG.debug("Request to delete UserProfile : {}", id);
        userProfileRepository.deleteById(id);
    }

    /**
     * Lay profile hien tai tu JWT, neu chua co thi tu dong khoi tao moi voi ID
     * trung voi keycloak sub
     * 
     */
    @Cacheable(value = "userProfile", key = "#jwt.subject")
    public UserProfileDTO getOrCreateProfile(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userProfileRepository.findById(userId).map(userProfileMapper::toDto)
                .orElseGet(() -> createProfileFromJwt(jwt, userId));
    }

    public UserProfileDTO createProfileFromJwt(Jwt jwt, UUID userId) {
        LOG.info("create new user profile for keycloak user sub: {}", userId);
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String username = StringUtils.hasText(preferredUsername) ? preferredUsername : userId.toString();
        String displayName = resolveDisplayName(jwt, username);
        UserProfile newProfile = new UserProfile();
        newProfile.setId(userId);
        newProfile.setUsername(username);
        newProfile.setDisplayName(displayName);
        newProfile.setCreatedAt(Instant.now());
        try {
            UserProfile saved = userProfileRepository.save(newProfile);
            return userProfileMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            LOG.warn("Concurrent profile creation detected for user sub: {}, query again from DB", userId);
            return userProfileRepository.findById(userId)
                    .map(userProfileMapper::toDto)
                    .orElseThrow(() -> e);
        }
    }

    private String resolveDisplayName(Jwt jwt, String fallbackUsername) {
        String name = jwt.getClaimAsString("name");
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        StringBuilder fullName = new StringBuilder();
        if (StringUtils.hasText(givenName)) {
            fullName.append(givenName.trim());
        }
        if (StringUtils.hasText(familyName)) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(familyName.trim());
        }
        return fullName.length() > 0 ? fullName.toString() : fallbackUsername;

    }

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> searchUsers(String query, Pageable pageable, Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        Page<UserProfile> profiles = userProfileRepository
                .findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(query, query, pageable);

        if (profiles.isEmpty()) {
            return Page.empty(pageable);
        }
        List<UUID> targetUserId = profiles.stream()
                .map(UserProfile::getId)
                .filter(id -> !id.equals(currentUserId))
                .collect(Collectors.toList());

        Set<UUID> friendIds = friendshipRepository.findFriendIdsIn(currentUserId, targetUserId);

        Set<UUID> pendingSentIds = friendRequestRepository.findPendingReceiverIdsIn(currentUserId, targetUserId,
                FriendRequestStatus.PENDING);
        Set<UUID> pendingReceivedIds = friendRequestRepository.findPendingSenderIdsIn(currentUserId, targetUserId,
                FriendRequestStatus.PENDING);

        Map<UUID, Long> mutualFriendsMap = friendshipRepository.countMutualFriendsForUsers(currentUserId, targetUserId)
                .stream().collect(Collectors.toMap(t -> t.get(0, UUID.class), t -> t.get(1, Long.class)));
        return profiles.map(profile -> {
            UserSearchDTO dto = new UserSearchDTO();
            dto.setId(profile.getId());
            dto.setUsername(profile.getUsername());
            dto.setDisplayName(profile.getDisplayName());
            dto.setBio(profile.getBio());
            dto.setEducation(profile.getEducation());
            dto.setWorkplace(profile.getWork());
            dto.setLocation(profile.getLocation());
            dto.setAvatarMediaId(profile.getAvatarMediaId());

            if (profile.getId().equals(currentUserId)) {
                dto.setFriendshipStatus("SELF");
                dto.setMutualFriendsCount(0);
                return dto;
            }

            dto.setMutualFriendsCount(mutualFriendsMap.getOrDefault(profile.getId(), 0L));

            if (friendIds.contains(profile.getId())) {
                dto.setFriendshipStatus("FRIEND");
            } else if (pendingSentIds.contains(profile.getId())) {
                dto.setFriendshipStatus("PENDING_SENT");
            } else if (pendingReceivedIds.contains(profile.getId())) {
                dto.setFriendshipStatus("PENDING_RECEIVED");
            } else {
                dto.setFriendshipStatus("NONE");
            }
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDetailDTO> getUserProfileDetails(UUID targetUserId, Jwt jwt) {
        return userProfileRepository.findById(targetUserId).map(profile -> {
            UserProfileDetailDTO dto = new UserProfileDetailDTO();
            dto.setId(profile.getId());
            dto.setUsername(profile.getUsername());
            dto.setDisplayName(profile.getDisplayName());
            dto.setAvatarMediaId(profile.getAvatarMediaId());
            dto.setCoverMediaId(profile.getCoverMediaId());
            dto.setBio(profile.getBio());
            dto.setEducation(profile.getEducation());
            dto.setWorkplace(profile.getWork());
            dto.setLocation(profile.getLocation());
            dto.setRelationship(profile.getRelationship());

            if (jwt != null && StringUtils.hasText(jwt.getSubject())) {
                UUID currentUserId = UUID.fromString(jwt.getSubject());

                // 1. Xem hồ sơ chính mình
                if (profile.getId().equals(currentUserId)) {
                    dto.setFriendshipStatus("SELF");
                    dto.setMutualFriendsCount(0);
                    return dto;
                }

                // 2. Đếm số bạn chung
                long mutual = friendshipRepository.countMutualFriends(currentUserId, profile.getId());
                dto.setMutualFriendsCount(mutual);

                // 3. Xác định trạng thái kết bạn & lấy friendRequestId nếu có
                if (friendshipRepository.existsFriendship(currentUserId, profile.getId())) {
                    dto.setFriendshipStatus("FRIEND");
                } else {
                    var sentReq = friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                            currentUserId, profile.getId(), FriendRequestStatus.PENDING);
                    if (sentReq.isPresent()) {
                        dto.setFriendshipStatus("PENDING_SENT");
                        dto.setFriendRequestId(sentReq.get().getId());
                    } else {
                        var recvReq = friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                                profile.getId(), currentUserId, FriendRequestStatus.PENDING);
                        if (recvReq.isPresent()) {
                            dto.setFriendshipStatus("PENDING_RECEIVED");
                            dto.setFriendRequestId(recvReq.get().getId());
                        } else {
                            dto.setFriendshipStatus("NONE");
                        }
                    }
                }
            } else {
                dto.setFriendshipStatus("NONE");
                dto.setMutualFriendsCount(0);
            }
            return dto;
        });
    }
    @Transactional(readOnly = true)
    public List<UserSearchDTO> getFriendSuggestions(Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<FriendSuggestionProjection> suggestions = friendshipRepository
                .findFriendSuggestions(currentUserId, PageRequest.of(0, 100));

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> suggestionIds = suggestions.stream().map(FriendSuggestionProjection::getUserId).toList();
        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(suggestionIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p));
        Map<UUID, Long> mutualFriendsMap = suggestions.stream()
                .collect(Collectors.toMap(
                        FriendSuggestionProjection::getUserId,
                        FriendSuggestionProjection::getMutualFriendsCount));

        return suggestionIds.stream()
                .map(profileMap::get)
                .filter(Objects::nonNull)
                .map(profile -> {
                    UserSearchDTO dto = new UserSearchDTO();
                    dto.setId(profile.getId());
                    dto.setUsername(profile.getUsername());
                    dto.setDisplayName(profile.getDisplayName());
                    dto.setBio(profile.getBio());
                    dto.setEducation(profile.getEducation());
                    dto.setWorkplace(profile.getWork());
                    dto.setLocation(profile.getLocation());
                    dto.setAvatarMediaId(profile.getAvatarMediaId());
                    dto.setFriendshipStatus("NONE");
                    dto.setMutualFriendsCount(mutualFriendsMap.getOrDefault(profile.getId(), 0L));
                    return dto;
                })
                .toList();
    }
}