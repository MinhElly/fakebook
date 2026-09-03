package com.minh.fakebook.post.service.mapper;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.dto.PostMediaDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PostMedia} and its DTO {@link PostMediaDTO}.
 */
@Mapper(componentModel = "spring")
public interface PostMediaMapper extends EntityMapper<PostMediaDTO, PostMedia> {
    @Mapping(target = "post", source = "post", qualifiedByName = "postId")
    PostMediaDTO toDto(PostMedia s);

    @Named("postId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PostDTO toDtoPostId(Post post);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
