package com.minh.fakebook.user.service.mapper;

import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Friendship} and its DTO {@link FriendshipDTO}.
 */
@Mapper(componentModel = "spring")
public interface FriendshipMapper extends EntityMapper<FriendshipDTO, Friendship> {
    @Mapping(target = "user", source = "user", qualifiedByName = "userProfileId")
    @Mapping(target = "friend", source = "friend", qualifiedByName = "userProfileId")
    FriendshipDTO toDto(Friendship s);

    @Named("userProfileId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    UserProfileDTO toDtoUserProfileId(UserProfile userProfile);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
