package com.minh.fakebook.gateway.web.rest;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import com.minh.fakebook.gateway.IntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
class BusinessBoundaryIT {
    @Autowired WebTestClient client;

    @Test void guestsCannotReachSavedFriendsMetadataOrReactorIdentities() {
        for (String path : List.of("/services/postservice/api/posts/saved", "/services/feedservice/api/feed/friends",
                "/services/mediaservice/api/media/" + UUID.randomUUID(),
                "/services/postservice/api/post-reactions/posts/" + UUID.randomUUID())) {
            client.get().uri(path).exchange().expectStatus().isUnauthorized();
        }
    }

    @Test void ordinaryJwtCannotReachServiceInternalApi() {
        client.mutateWith(mockJwt().jwt(t -> t.subject(UUID.randomUUID().toString())))
            .post().uri("/services/postservice/api/internal/feed-posts/readable")
            .exchange().expectStatus().isForbidden();
    }
}
