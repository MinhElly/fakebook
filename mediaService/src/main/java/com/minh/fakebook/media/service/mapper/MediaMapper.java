package com.minh.fakebook.media.service.mapper;

import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.service.dto.MediaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Media} and its DTO {@link MediaDTO}.
 */
@Mapper(componentModel = "spring")
public interface MediaMapper extends EntityMapper<MediaDTO, Media> {}
