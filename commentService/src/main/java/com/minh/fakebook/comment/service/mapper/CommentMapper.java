package com.minh.fakebook.comment.service.mapper;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Comment} and its DTO {@link CommentDTO}.
 */
@Mapper(componentModel = "spring")
public interface CommentMapper extends EntityMapper<CommentDTO, Comment> {
    @Mapping(target = "parentComment", source = "parentComment", qualifiedByName = "commentId")
    CommentDTO toDto(Comment s);

    @Named("commentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    CommentDTO toDtoCommentId(Comment comment);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
