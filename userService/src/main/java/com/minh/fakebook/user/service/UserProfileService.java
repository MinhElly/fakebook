package com.minh.fakebook.user.service;

import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.user.domain.UserProfile}.
 */
@Service
@Transactional
public class UserProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileRepository userProfileRepository, UserProfileMapper userProfileMapper) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * Save a userProfile.
     *
     * @param userProfileDTO the entity to save.
     * @return the persisted entity.
     */
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
    public Optional<UserProfileDTO> findOne(UUID id) {
        LOG.debug("Request to get UserProfile : {}", id);
        return userProfileRepository.findById(id).map(userProfileMapper::toDto);
    }

    /**
     * Delete the userProfile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete UserProfile : {}", id);
        userProfileRepository.deleteById(id);
    }

    /**
     * Lay profile hien tai tu JWT, neu chua co thi tu dong khoi tao moi voi ID trung voi keycloak sub
     * 
     */
    public UserProfileDTO getOrCreateProfile(Jwt jwt){
        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        return userProfileRepository.findById(userId).map(userProfileMapper::toDto)
        .orElseGet(()->{LOG.info("create new user profile for keycloak user sub: {}", userId);
            UserProfile newProfile = new UserProfile();
            newProfile.setId(userId);
            newProfile.setUsername(username != null ? username : userId.toString());
            String name = jwt.getClaimAsString("name");
            newProfile.setDisplayName(name != null ? name : newProfile.getUsername());
            newProfile.setCreatedAt(Instant.now());
            return userProfileMapper.toDto(userProfileRepository.save(newProfile));    
        });
    }
}
