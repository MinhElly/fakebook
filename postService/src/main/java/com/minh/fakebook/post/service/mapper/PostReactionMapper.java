package com.minh.fakebook.post.service.mapper;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PostReaction} and its DTO {@link PostReactionDTO}.
 */
@Mapper(componentModel = "spring")
public interface PostReactionMapper extends EntityMapper<PostReactionDTO, PostReaction> {
    @Mapping(target = "post", source = "post", qualifiedByName = "postId")
    PostReactionDTO toDto(PostReaction s);

    @Named("postId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PostDTO toDtoPostId(Post post);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
