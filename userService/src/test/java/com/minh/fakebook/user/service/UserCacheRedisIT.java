package com.minh.fakebook.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.minh.fakebook.user.UserServiceApp;
import com.minh.fakebook.user.config.*;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(classes = {UserServiceApp.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class,
    JacksonHibernateConfiguration.class}, properties = {"spring.datasource.url=jdbc:h2:mem:user-cache-proof;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.data.redis.timeout=500ms", "spring.data.redis.connect-timeout=500ms"})
class UserCacheRedisIT {
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10.1").withExposedPorts(6379);
    static { REDIS.start(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }
    @Autowired private UserProfileRepository profiles;
    @Autowired private FriendRequestService requests;
    @Autowired private FollowService follows;
    @Autowired private UserProfileService profileService;
    @Autowired private CacheManager caches;

    @Test void committedChangesEvictBothUsersPreserveUnrelatedKeysAndReadsSurviveRedisOutage() {
        UUID sender = profile(), receiver = profile(), unrelated = UUID.randomUUID();
        PageRequest ascending = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        PageRequest descending = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        var request = requests.sendFriendRequest(sender, receiver);
        assertThat(requests.getSentPendingRequests(sender, ascending).getTotalElements()).isEqualTo(1);
        assertThat(requests.getReceivedPendingRequests(receiver, descending).getTotalElements()).isEqualTo(1);
        assertThat(requests.getSentPendingRequests(sender, ascending).getSort()).isEqualTo(ascending.getSort());
        var cache = caches.getCache("pendingSentRequests");
        String sentinel = unrelated + "_0_10_UNSORTED";
        cache.put(sentinel, "unrelated");
        requests.rejectFriendRequest(request.getId(), receiver);
        assertThat(requests.getSentPendingRequests(sender, ascending)).isEmpty();
        assertThat(requests.getReceivedPendingRequests(receiver, descending)).isEmpty();
        assertThat(cache.get(sentinel, String.class)).isEqualTo("unrelated");
        request = requests.sendFriendRequest(sender, receiver);
        requests.getSentPendingRequests(sender, ascending);
        requests.getReceivedPendingRequests(receiver, descending);
        requests.cancelFriendRequest(request.getId(), sender);
        assertThat(requests.getSentPendingRequests(sender, ascending)).isEmpty();
        assertThat(requests.getReceivedPendingRequests(receiver, descending)).isEmpty();
        follows.followUser(sender, receiver);
        follows.getFollowingList(sender, ascending);
        var dto = profileService.findOne(receiver).orElseThrow();
        dto.setDisplayName("Changed profile");
        profileService.update(dto);
        assertThat(follows.getFollowingList(sender, ascending).getContent().getFirst().getFollowing().getDisplayName())
            .isEqualTo("Changed profile");
        REDIS.stop();
        assertThat(requests.getSentPendingRequests(sender, ascending)).isEmpty();
        assertThat(follows.getFollowingList(sender, descending).getTotalElements()).isEqualTo(1);
        assertThat(profileService.findOne(receiver)).isPresent();
    }
    private UUID profile() {
        UUID id = UUID.randomUUID();
        profiles.saveAndFlush(new UserProfile().id(id).username("cache-" + id).displayName("Cache test").createdAt(Instant.now()));
        return id;
    }
}
