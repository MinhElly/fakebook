package com.minh.fakebook.post.service.mapper;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.service.dto.PostDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Post} and its DTO {@link PostDTO}.
 */
@Mapper(componentModel = "spring")
public interface PostMapper extends EntityMapper<PostDTO, Post> {}
