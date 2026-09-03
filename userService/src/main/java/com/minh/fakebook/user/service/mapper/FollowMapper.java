package com.minh.fakebook.user.service.mapper;

import com.minh.fakebook.user.domain.Follow;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.service.dto.FollowDTO;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Follow} and its DTO {@link FollowDTO}.
 */
@Mapper(componentModel = "spring")
public interface FollowMapper extends EntityMapper<FollowDTO, Follow> {
    @Mapping(target = "follower", source = "follower", qualifiedByName = "userProfileId")
    @Mapping(target = "following", source = "following", qualifiedByName = "userProfileId")
    FollowDTO toDto(Follow s);

    @Named("userProfileId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    UserProfileDTO toDtoUserProfileId(UserProfile userProfile);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
