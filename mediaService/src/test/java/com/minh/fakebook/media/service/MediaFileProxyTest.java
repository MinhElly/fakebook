package com.minh.fakebook.media.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.net.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;

class MediaFileProxyTest {
    @Test void forwardsRangeAndClosesUpstreamWithoutBuffering() throws Exception {
        var connection = mock(HttpURLConnection.class);
        var proxy = spy(new MediaFileProxy());
        doReturn(connection).when(proxy).openConnection(any());
        when(connection.getResponseCode()).thenReturn(206);
        when(connection.getHeaderField("Content-Range")).thenReturn("bytes 10-12/100");
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        var response = proxy.fetch("https://res.cloudinary.com/test/video.mp4", "bytes=10-12");
        assertThat(response.getStatusCode().value()).isEqualTo(206);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes 10-12/100");
        verify(connection).setRequestProperty("Range", "bytes=10-12");
        verify(connection, never()).disconnect();
        try (var stream = ((Resource)response.getBody()).getInputStream()) { assertThat(stream.readAllBytes()).containsExactly(1,2,3); }
        verify(connection).disconnect();
    }
    @Test void preservesUnsatisfiableRangeAndRejectsOtherOrigins() throws Exception {
        var connection = mock(HttpURLConnection.class); var proxy = spy(new MediaFileProxy());
        doReturn(connection).when(proxy).openConnection(any());
        when(connection.getResponseCode()).thenReturn(416);
        when(connection.getHeaderField("Content-Range")).thenReturn("bytes */100");
        var response = proxy.fetch("https://res.cloudinary.com/test", "bytes=900-");
        assertThat(response.getStatusCode().value()).isEqualTo(416);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes */100");
        assertThatThrownBy(() -> proxy.fetch("http://localhost/private", null)).isInstanceOf(ResponseStatusException.class);
        assertThat(proxy.fetch("https://res.cloudinary.com/test", "bytes=1-2,3-4").getStatusCode().value()).isEqualTo(416);
    }
}
