package com.minh.fakebook.comment.service.mapper;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.CommentReaction;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.dto.CommentReactionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CommentReaction} and its DTO {@link CommentReactionDTO}.
 */
@Mapper(componentModel = "spring")
public interface CommentReactionMapper extends EntityMapper<CommentReactionDTO, CommentReaction> {
    @Mapping(target = "comment", source = "comment", qualifiedByName = "commentId")
    CommentReactionDTO toDto(CommentReaction s);

    @Named("commentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    CommentDTO toDtoCommentId(Comment comment);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
