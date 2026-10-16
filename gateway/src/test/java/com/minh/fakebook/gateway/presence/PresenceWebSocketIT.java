package com.minh.fakebook.gateway.presence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.minh.fakebook.gateway.client.UserPresenceClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real sockets and Redis; UserService permissions are controlled here and tested against SQL in its own IT. */
class PresenceWebSocketIT {
    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final String ORIGIN = "http://localhost:5173";
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void ticketIsSingleUseAndCannotOutliveAuthentication() {
        withEnvironment(env -> {
            UUID user = UUID.randomUUID();
            String ticket = env.ticket(user);
            assertThat(env.tickets.consume(ticket).block(WAIT).userId()).isEqualTo(user);
            assertThatThrownBy(() -> env.tickets.consume(ticket).block(WAIT)).hasMessageContaining("consumed");
            assertThatThrownBy(() -> env.tickets.issue(jwt(user, Instant.now().minusSeconds(1))).block(WAIT))
                .hasMessageContaining("401");
        });
    }

    @Test
    void twoGatewaysSharePresenceAndLastTabCloseBecomesOffline() {
        withEnvironment(Duration.ofMinutes(5), env -> {
            UUID observer = UUID.randomUUID(), friend = UUID.randomUUID();
            env.allow(observer, friend);
            try (var watching = env.connect(0, observer); var first = env.connect(0, friend); var second = env.connect(1, friend)) {
                watching.subscribe(friend);
                assertThat(watching.status(friend, "online").get("lastSeenAt").asText()).isNotBlank();
                env.ensureBusReady(watching);
                assertThat(env.store.snapshot(friend).block(WAIT).online()).isTrue();
                first.close();
                assertThat(env.store.snapshot(friend).block(WAIT).online()).isTrue();
                second.close();
                watching.status(friend, "offline", "PRESENCE_CHANGED");
                assertThat(env.store.snapshot(friend).block(WAIT).online()).isFalse();
                assertThat(env.store.findPending(100).collectList().block(WAIT)).contains(friend);
            }
        });
    }

    @Test
    void pongRenewsLeaseButUnsolicitedPongCannotKeepExpiredConnectionAlive() {
        withEnvironment(env -> {
            UUID user = UUID.randomUUID();
            try (var socket = env.connect(0, user)) {
                Instant before = env.store.snapshot(user).block(WAIT).lastSeenAt();
                await(() -> socket.pings.get() > 1);
                await(() -> env.store.snapshot(user).block(WAIT).lastSeenAt().isAfter(before));
                socket.pong = false;
                var members = env.redis.opsForZSet().range(env.prefix + ":user:" + user + ":connections", org.springframework.data.domain.Range.closed(0L, -1L)).collectList().block(WAIT);
                env.redis.opsForZSet().add(env.prefix + ":user:" + user + ":connections", members.getFirst(), 1D).block(WAIT);
                socket.socket.sendPong(ByteBuffer.wrap("forged".getBytes())).join();
                assertThat(env.store.snapshot(user).block(WAIT).online()).isFalse();
                env.store.pruneExpired(user).block(WAIT);
                assertThat(env.store.findPending(100).collectList().block(WAIT)).contains(user);
            }
        });
    }

    @Test
    void revocationAndMissedPubsubAreReconciledFromFreshPermissions() {
        withEnvironment(env -> {
            UUID observer = UUID.randomUUID(), friend = UUID.randomUUID();
            env.allow(observer, friend);
            try (var watching = env.connect(0, observer); var target = env.connect(1, friend)) {
                watching.subscribe(friend); watching.status(friend, "online");
                env.permissions.get(observer).remove(friend);
                // Deliberately do not publish: the periodic snapshot must remove private state.
                watching.awaitRemoved(friend);
                env.allow(observer, friend);
                watching.status(friend, "online");
                env.permissions.get(observer).remove(friend);
                env.bus.invalidateFriendships().block(WAIT);
                watching.awaitRemoved(friend);
            }
        });
    }

    @Test
    void permissionOutageRemovesKnownStateAndRecoveryRestoresIt() {
        withEnvironment(env -> {
            UUID observer = UUID.randomUUID(), friend = UUID.randomUUID();
            env.allow(observer, friend);
            try (var watching = env.connect(0, observer); var target = env.connect(1, friend)) {
                watching.subscribe(friend); watching.status(friend, "online");
                env.failPermissions = true;
                watching.awaitType("PRESENCE_REMOVED");
                watching.awaitType("ERROR");
                env.failPermissions = false;
                watching.status(friend, "online");
            }
        });
    }

    @Test
    void rejectsOriginMissingAuthReusedTicketAndCrossAccountReauthentication() {
        withEnvironment(env -> {
            UUID user = UUID.randomUUID();
            try (var invalid = env.raw(0, "https://untrusted.example")) {
                await(() -> invalid.closed);
                assertThat(env.store.snapshot(user).block(WAIT).online()).isFalse();
            }
            try (var unauthenticated = env.raw(0, ORIGIN)) { await(() -> unauthenticated.closed); }
            String ticket = env.ticket(user);
            try (var first = env.raw(0, ORIGIN); var replay = env.raw(1, ORIGIN)) {
                first.send(Map.of("type", "AUTH", "ticket", ticket)); first.awaitType("READY");
                replay.send(Map.of("type", "AUTH", "ticket", ticket)); await(() -> replay.closed);
                first.send(Map.of("type", "REAUTH", "ticket", env.ticket(UUID.randomUUID())));
                await(() -> first.closed);
            }
        });
    }

    @Test
    void expiredJwtClosesSocketAndSameAccountCanRenewWithFreshTicket() {
        withEnvironment(env -> {
            UUID user = UUID.randomUUID();
            try (var socket = env.connect(0, user)) {
                // Warm the real socket/Redis/codecs before testing a short expiry window.
                String ticket = env.tickets.issue(jwt(user, Instant.now().plusSeconds(3))).block(WAIT).getBody().ticket();
                socket.send(Map.of("type", "REAUTH", "ticket", ticket)); socket.awaitType("REAUTHENTICATED");
                socket.send(Map.of("type", "REAUTH", "ticket", env.ticket(user))); socket.awaitType("REAUTHENTICATED");
                socket.send(Map.of("type", "REAUTH", "ticket", env.tickets.issue(jwt(user, Instant.now().plusSeconds(2))).block(WAIT).getBody().ticket()));
                socket.awaitType("REAUTHENTICATED"); await(() -> socket.closed);
            }
        });
    }

    @Test
    void enforcesSubscriptionAndInboundFrameLimits() {
        withEnvironment(env -> {
            try (var socket = env.connect(0, UUID.randomUUID())) {
                socket.send(Map.of("type", "SUBSCRIBE", "userIds", java.util.stream.IntStream.range(0, 101)
                    .mapToObj(index -> UUID.randomUUID()).toList()));
                await(() -> socket.closed);
            }
            try (var socket = env.connect(1, UUID.randomUUID())) {
                socket.socket.sendText("x".repeat(9000), true).join();
                await(() -> socket.closed);
            }
        });
    }

    private void withEnvironment(java.util.function.Consumer<Environment> test) {
        withEnvironment(Duration.ofMillis(400), test);
    }

    private void withEnvironment(Duration reconciliationInterval, java.util.function.Consumer<Environment> test) {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class))
            .withPropertyValues("spring.data.redis.url=" + System.getenv().getOrDefault("REDIS_URL", "redis://localhost:6379"))
            .run(context -> {
                try (var env = new Environment(context.getBean(ReactiveStringRedisTemplate.class), reconciliationInterval)) { test.accept(env); }
            });
    }

    private static Jwt jwt(UUID user, Instant expiry) {
        return Jwt.withTokenValue("test-token").header("alg", "none").subject(user.toString()).expiresAt(expiry).build();
    }

    private static void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            try { Thread.sleep(20); } catch (InterruptedException error) { throw new AssertionError(error); }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private final class Environment implements AutoCloseable {
        final String prefix = "fakebook:presence:ws-test:" + UUID.randomUUID();
        final ReactiveStringRedisTemplate redis;
        final PresenceRedisStore store;
        final PresenceTickets tickets;
        final PresenceBus bus;
        final List<DisposableServer> servers = new ArrayList<>();
        final Map<UUID, Set<UUID>> permissions = new ConcurrentHashMap<>();
        volatile boolean failPermissions;

        Environment(ReactiveStringRedisTemplate redis, Duration reconciliationInterval) {
            this.redis = redis; store = new PresenceRedisStore(redis, prefix);
            tickets = new PresenceTickets(redis, prefix); bus = new PresenceBus(redis, store);
            var users = mock(UserPresenceClient.class);
            when(users.readable(any(), anySet())).thenAnswer(invocation -> Mono.defer(() -> {
                if (failPermissions) return Mono.error(new IllegalStateException("Permission API down"));
                UUID viewer = invocation.getArgument(0); Set<UUID> targets = invocation.getArgument(1);
                return Mono.just(targets.stream().filter(id -> viewer.equals(id) || permissions.getOrDefault(viewer, Set.of()).contains(id))
                    .map(id -> new UserPresenceClient.ReadablePresence(id, null)).toList());
            }));
            for (int index = 0; index < 2; index++) {
                var perGatewayStore = new PresenceRedisStore(redis, prefix);
                var handler = new PresenceWebSocketHandler(perGatewayStore, tickets, users, new PresenceBus(redis, perGatewayStore), mapper,
                    ORIGIN, Duration.ofMillis(200), reconciliationInterval);
                var service = new HandshakeWebSocketService(new ReactorNettyRequestUpgradeStrategy(() ->
                    reactor.netty.http.server.WebsocketServerSpec.builder().maxFramePayloadLength(8192)));
                var webHandler = (org.springframework.web.server.adapter.HttpWebHandlerAdapter) WebHttpHandlerBuilder
                    .webHandler(exchange -> service.handleRequest(exchange, handler)).build();
                // Real Boot initializes codecs during startup, before serving Netty requests.
                webHandler.setCodecConfigurer(org.springframework.http.codec.ServerCodecConfigurer.create());
                var adapter = new ReactorHttpHandlerAdapter(webHandler);
                servers.add(HttpServer.create().host("127.0.0.1").port(0).handle(adapter).bindNow(WAIT));
            }
        }

        void allow(UUID viewer, UUID friend) { permissions.computeIfAbsent(viewer, key -> ConcurrentHashMap.newKeySet()).add(friend); }
        void ensureBusReady(Client observer) {
            // Redis subscription acknowledgement is asynchronous. Prove that the observer's
            // Gateway receives hints before asserting delivery of a particular transition.
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                bus.invalidateFriendships().block(WAIT);
                try {
                    String message = observer.messages.poll(200, TimeUnit.MILLISECONDS);
                    if (message != null && "PRESENCE_CHANGED".equals(mapper.readTree(message).path("type").asText())) return;
                } catch (InterruptedException error) { throw new AssertionError(error); }
            }
            throw new AssertionError("Redis hint subscription not ready");
        }
        String ticket(UUID user) { return tickets.issue(jwt(user, Instant.now().plusSeconds(300))).block(WAIT).getBody().ticket(); }
        Client raw(int gateway, String origin) { return new Client(servers.get(gateway).port(), origin); }
        Client connect(int gateway, UUID user) {
            Client client = raw(gateway, ORIGIN);
            client.send(Map.of("type", "AUTH", "ticket", ticket(user))); client.awaitType("READY");
            return client;
        }
        public void close() {
            servers.forEach(server -> server.disposeNow(WAIT));
            redis.keys(prefix + ":*").flatMap(redis::delete).then().block(WAIT);
        }
    }

    private final class Client implements WebSocket.Listener, AutoCloseable {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final AtomicInteger pings = new AtomicInteger();
        volatile boolean closed;
        volatile boolean pong = true;
        final HttpClient http = HttpClient.newHttpClient();
        final WebSocket socket;
        final StringBuilder partial = new StringBuilder();
        Client(int port, String origin) {
            socket = http.newWebSocketBuilder().header("Origin", origin)
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/api/presence/ws"), this).join();
        }
        public void onOpen(WebSocket socket) { socket.request(1); }
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) { messages.add(partial.toString()); partial.setLength(0); }
            socket.request(1); return null;
        }
        public CompletionStage<?> onPing(WebSocket socket, ByteBuffer data) {
            pings.incrementAndGet(); socket.request(1); return pong ? socket.sendPong(data) : null;
        }
        public CompletionStage<?> onClose(WebSocket socket, int code, String reason) { closed = true; return null; }
        public void onError(WebSocket socket, Throwable error) { closed = true; }
        void send(Object value) { socket.sendText(mapper.writeValueAsString(value), true).join(); }
        void subscribe(UUID user) { send(Map.of("type", "SUBSCRIBE", "userIds", List.of(user))); }
        JsonNode awaitType(String type) {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                try {
                    String value = messages.poll(100, TimeUnit.MILLISECONDS);
                    if (value != null) { JsonNode node = mapper.readTree(value); if (type.equals(node.path("type").asText())) return node; }
                    else if (closed) throw new AssertionError("Socket closed before " + type);
                } catch (InterruptedException error) { throw new AssertionError(error); }
            }
            throw new AssertionError("Missing presence frame " + type);
        }
        JsonNode status(UUID user, String status) {
            return status(user, status, "PRESENCE_SNAPSHOT");
        }
        JsonNode status(UUID user, String status, String type) {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                JsonNode message = awaitType(type);
                for (JsonNode row : message.path("entries")) {
                    if (user.toString().equals(row.path("userId").asText()) && status.equals(row.path("status").asText())) return row;
                }
            }
            throw new AssertionError("Missing presence status " + status);
        }
        void awaitRemoved(UUID user) {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                JsonNode message = awaitType("PRESENCE_SNAPSHOT");
                for (JsonNode removed : message.path("removed")) if (user.toString().equals(removed.asText())) return;
            }
            throw new AssertionError("Missing permission removal");
        }
        public void close() {
            if (!closed) { socket.sendClose(WebSocket.NORMAL_CLOSURE, "test finished").join(); await(() -> closed); }
            http.close();
        }
    }
}
