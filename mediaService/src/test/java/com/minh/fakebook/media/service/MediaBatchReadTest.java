package com.minh.fakebook.media.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.*;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MediaBatchReadTest {
    @Test void batchAuthorizationDoesNotExposeOtherOwnersOrUnreadableParents() {
        UUID owner=UUID.randomUUID(), other=UUID.randomUUID();
        var own = new Media().id(UUID.randomUUID()).ownerId(owner).status(MediaStatus.ACTIVE); own.setPurpose(MediaPurpose.POST);
        var denied = new Media().id(UUID.randomUUID()).ownerId(other).status(MediaStatus.ACTIVE); denied.setPurpose(MediaPurpose.POST);
        var repository=mock(MediaRepository.class); var mapper=mock(MediaMapper.class); var access=mock(PostMediaAccess.class);
        var ids=List.of(own.getId(),denied.getId()); when(repository.findAllById(ids)).thenReturn(List.of(own,denied));
        when(access.batch(ids)).thenReturn(Map.of(own.getId(),new PostMediaAccess.Grant(false,false),denied.getId(),new PostMediaAccess.Grant(true,false)));
        var dto=new MediaDTO(); dto.setId(own.getId()); when(mapper.toDto(own)).thenReturn(dto);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.toString(),"",List.of()));
        try {
            var result=new MediaService(repository,mapper,mock(FileStorageService.class),access).findReadableBatch(ids);
            assertThat(result).extracting(MediaDTO::getId).containsExactly(own.getId());
            verify(access,times(1)).batch(ids); verify(mapper,never()).toDto(denied);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
