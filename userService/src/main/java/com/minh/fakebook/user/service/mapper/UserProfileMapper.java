package com.minh.fakebook.user.service.mapper;

import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserProfileMapper
    extends EntityMapper<UserProfileDTO, UserProfile> {

    @Override
    @Mapping(target = "lastSeenAt", ignore = true)
    UserProfile toEntity(UserProfileDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @BeanMapping(
        nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.SET_TO_NULL
    )
    void updateProfile(
        @MappingTarget UserProfile entity,
        UserProfileDTO dto
    );

    @Override
    @Named("partialUpdate")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @BeanMapping(
        nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.IGNORE
    )
    void partialUpdate(
        @MappingTarget UserProfile entity,
        UserProfileDTO dto
    );
}
