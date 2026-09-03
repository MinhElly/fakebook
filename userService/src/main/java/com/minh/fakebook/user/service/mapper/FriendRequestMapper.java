package com.minh.fakebook.user.service.mapper;

import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link FriendRequest} and its DTO {@link FriendRequestDTO}.
 */
@Mapper(componentModel = "spring")
public interface FriendRequestMapper extends EntityMapper<FriendRequestDTO, FriendRequest> {
    @Mapping(target = "sender", source = "sender", qualifiedByName = "userProfileId")
    @Mapping(target = "receiver", source = "receiver", qualifiedByName = "userProfileId")
    FriendRequestDTO toDto(FriendRequest s);

    @Named("userProfileId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    UserProfileDTO toDtoUserProfileId(UserProfile userProfile);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
