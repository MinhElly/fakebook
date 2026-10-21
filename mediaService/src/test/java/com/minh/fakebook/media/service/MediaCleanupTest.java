package com.minh.fakebook.media.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;

class MediaCleanupTest {
    @Test void referencedPostMediaIsNeverPhysicallyDeleted() {
        var repository = mock(MediaRepository.class); var storage = mock(FileStorageService.class); var access = mock(PostMediaAccess.class);
        UUID id = UUID.randomUUID(); var media = new Media().id(id).storageKey("shared").status(MediaStatus.ACTIVE);
        media.setPurpose(com.minh.fakebook.media.domain.enumeration.MediaPurpose.POST);
        when(repository.findById(id)).thenReturn(Optional.of(media)); when(access.isAttached(id)).thenReturn(true);
        new MediaService(repository, mock(MediaMapper.class), storage, access).deleteBySystem(id);
        verifyNoInteractions(storage); verify(repository, never()).save(any());
    }
    @Test void storageFailureDoesNotMarkCleanupComplete() throws Exception {
        var repository = mock(MediaRepository.class);
        var storage = mock(FileStorageService.class);
        UUID id = UUID.randomUUID();
        var media = new Media().id(id).storageKey("stable-key").status(MediaStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(media));
        doThrow(new IOException("Storage unavailable")).when(storage).deleteFile("stable-key");
        var service = new MediaService(repository, mock(MediaMapper.class), storage, mock(PostMediaAccess.class));
        assertThatThrownBy(() -> service.deleteBySystem(id)).isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
        assertThat(media.getStatus()).isEqualTo(MediaStatus.ACTIVE);
        verify(repository, never()).save(any());
        doNothing().when(storage).deleteFile("stable-key");
        service.deleteBySystem(id);
        assertThat(media.getStatus()).isEqualTo(MediaStatus.DELETED);
        verify(repository).save(media);
    }
    @Test void missingMediaReplayIsHarmless() {
        var repository = mock(MediaRepository.class);
        var storage = mock(FileStorageService.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        new MediaService(repository, mock(MediaMapper.class), storage, mock(PostMediaAccess.class)).deleteBySystem(id);
        verifyNoInteractions(storage);
        verify(repository, never()).save(any());
    }
}
