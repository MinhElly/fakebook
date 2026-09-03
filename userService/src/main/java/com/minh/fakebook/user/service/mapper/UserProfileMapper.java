package com.minh.fakebook.user.service.mapper;

import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link UserProfile} and its DTO {@link UserProfileDTO}.
 */
@Mapper(componentModel = "spring")
public interface UserProfileMapper extends EntityMapper<UserProfileDTO, UserProfile> {}
