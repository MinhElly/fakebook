package com.minh.fakebook.gateway.presence;

import com.minh.fakebook.gateway.client.UserPresenceClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

@Component
public class PresenceWebSocketHandler implements WebSocketHandler {
    private final PresenceRedisStore store;
    private final PresenceTickets tickets;
    private final UserPresenceClient users;
    private final PresenceBus bus;
    private final ObjectMapper mapper;
    private final Set<String> allowedOrigins;
    private final Duration pingInterval;
    private final Duration reconcileInterval;

    public PresenceWebSocketHandler(PresenceRedisStore store, PresenceTickets tickets, UserPresenceClient users,
        PresenceBus bus, ObjectMapper mapper,
        @Value("${presence.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
        @Value("${presence.ping-interval:20s}") Duration pingInterval,
        @Value("${presence.reconcile-interval:30s}") Duration reconcileInterval) {
        this.store = store; this.tickets = tickets; this.users = users; this.bus = bus; this.mapper = mapper;
        this.allowedOrigins = new HashSet<>(Arrays.asList(allowedOrigins.split("\\s*,\\s*")));
        this.pingInterval = pingInterval; this.reconcileInterval = reconcileInterval;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String origin = session.getHandshakeInfo().getHeaders().getOrigin();
        if (origin == null || !allowedOrigins.contains(origin) || session.getHandshakeInfo().getUri().getRawQuery() != null) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }
        return Mono.usingWhen(Mono.fromSupplier(() -> new Connection(session)), Connection::run,
            Connection::cleanup, (connection, error) -> connection.cleanup(), Connection::cleanup);
    }

    record Command(String type, String ticket, List<UUID> userIds) {}
    record Frame(WebSocketMessage.Type type, String payload) {}
    record Output(boolean ping, String payload) {}
    public record Entry(UUID userId, String status, Instant lastSeenAt) {}

    private final class Connection {
        private final WebSocketSession session;
        private final UUID connectionId = UUID.randomUUID();
        private final Sinks.Many<Output> outbound = Sinks.many().unicast()
            .onBackpressureBuffer(new java.util.concurrent.ArrayBlockingQueue<>(256));
        private final Sinks.Many<String> refreshes = Sinks.many().multicast().onBackpressureBuffer(8, false);
        private final Map<String, Long> challenges = new ConcurrentHashMap<>();
        private volatile PresenceTickets.Grant grant;
        private volatile Set<UUID> requested = Set.of();
        private volatile long subscriptionVersion;
        private volatile long lastPongNanos;
        private boolean registrationStarted;
        private long sequence;

        Connection(WebSocketSession session) { this.session = session; }

        Mono<Void> run() {
            Mono<Void> incoming = session.receive()
                .map(message -> {
                    if (message.getPayload().readableByteCount() > 8192) throw new IllegalArgumentException("Frame too large");
                    return new Frame(message.getType(), message.getPayloadAsText(StandardCharsets.UTF_8));
                }).concatMap(this::receive).then();
            Mono<Void> outgoing = session.send(outbound.asFlux().map(output -> output.ping()
                ? session.pingMessage(factory -> factory.wrap(output.payload().getBytes(StandardCharsets.UTF_8)))
                : session.textMessage(output.payload())));
            Mono<Void> authTimeout = Mono.delay(Duration.ofSeconds(5)).flatMap(ignored -> grant == null
                ? Mono.error(new IllegalArgumentException("AUTH timeout")) : Mono.never());
            Mono<Void> clock = Flux.interval(pingInterval).concatMap(ignored -> tick()).then();
            Mono<Void> updates = Flux.merge(
                refreshes.asFlux(),
                Flux.interval(reconcileInterval).map(ignored -> "PRESENCE_SNAPSHOT"),
                bus.changes().filter(id -> "*".equals(id) || requested.stream().anyMatch(user -> user.toString().equals(id)))
                    .map(ignored -> "PRESENCE_CHANGED")
            ).onBackpressureLatest().concatMap(this::refresh, 1).then();
            return Mono.firstWithSignal(incoming, outgoing, authTimeout, clock, updates)
                .onErrorResume(error -> session.close(error instanceof IllegalArgumentException
                    ? CloseStatus.POLICY_VIOLATION : CloseStatus.SERVER_ERROR));
        }

        Mono<Void> receive(Frame frame) {
            return Mono.defer(() -> {
                if (frame.type() == WebSocketMessage.Type.PONG) {
                    Long issued = challenges.remove(frame.payload());
                    if (grant == null || issued == null || System.nanoTime() - issued > Duration.ofSeconds(65).toNanos()) return Mono.empty();
                    if (!grant.authExpiresAt().isAfter(Instant.now())) return Mono.error(new IllegalArgumentException("Authentication expired"));
                    return store.heartbeat(grant.userId(), connectionId).flatMap(alive -> {
                        if (!alive) return Mono.error(new IllegalArgumentException("Connection lease expired"));
                        lastPongNanos = System.nanoTime();
                        return Mono.empty();
                    });
                }
                if (frame.type() == WebSocketMessage.Type.PING) return Mono.empty();
                if (frame.type() != WebSocketMessage.Type.TEXT) return Mono.error(new IllegalArgumentException("Text frames required"));
                Command command = mapper.readValue(frame.payload(), Command.class);
                if ("AUTH".equals(command.type()) || "REAUTH".equals(command.type())) {
                    boolean initial = "AUTH".equals(command.type());
                    if (initial != (grant == null)) return Mono.error(new IllegalArgumentException("Invalid authentication order"));
                    return tickets.consume(command.ticket()).flatMap(next -> {
                        if (!initial && !next.userId().equals(grant.userId())) return Mono.error(new IllegalArgumentException("Identity changed"));
                        return users.readable(next.userId(), Set.of(next.userId())).flatMap(rows -> {
                            if (rows.stream().noneMatch(row -> row.userId().equals(next.userId()))) {
                                return Mono.error(new IllegalArgumentException("Profile unavailable"));
                            }
                            grant = next;
                            Mono<?> register = Mono.empty();
                            if (initial) {
                                registrationStarted = true;
                                lastPongNanos = System.nanoTime();
                                register = store.register(next.userId(), connectionId);
                            }
                            return register.then(Mono.fromRunnable(() -> send(Map.of(
                                "type", initial ? "READY" : "REAUTHENTICATED", "epoch", connectionId,
                                "authExpiresAt", next.authExpiresAt()))));
                        });
                    });
                }
                if (grant == null || !grant.authExpiresAt().isAfter(Instant.now())) return Mono.error(new IllegalArgumentException("AUTH required"));
                if (command.userIds() == null || command.userIds().size() > 100 || command.userIds().contains(null)) {
                    return Mono.error(new IllegalArgumentException("Invalid subscription"));
                }
                Set<UUID> next = new HashSet<>(requested);
                if ("SUBSCRIBE".equals(command.type())) next.addAll(command.userIds());
                else if ("UNSUBSCRIBE".equals(command.type())) next.removeAll(command.userIds());
                else return Mono.error(new IllegalArgumentException("Unknown command"));
                if (next.size() > 100) return Mono.error(new IllegalArgumentException("Subscription limit exceeded"));
                requested = Set.copyOf(next);
                subscriptionVersion++;
                refreshes.tryEmitNext("PRESENCE_SNAPSHOT");
                return Mono.empty();
            });
        }

        Mono<Void> tick() {
            return Mono.defer(() -> {
                if (grant == null) return Mono.empty();
                if (!grant.authExpiresAt().isAfter(Instant.now()) || System.nanoTime() - lastPongNanos > Duration.ofSeconds(65).toNanos()) {
                    return Mono.error(new IllegalArgumentException("Session expired"));
                }
                if (grant.authExpiresAt().minusSeconds(45).isBefore(Instant.now())) send(Map.of("type", "REAUTH_REQUIRED"));
                long now = System.nanoTime();
                challenges.entrySet().removeIf(entry -> now - entry.getValue() > Duration.ofSeconds(65).toNanos());
                String nonce = UUID.randomUUID().toString();
                challenges.put(nonce, now);
                emit(new Output(true, nonce));
                return Mono.empty();
            });
        }

        Mono<Void> refresh(String type) {
            return Mono.defer(() -> {
                if (grant == null) return Mono.empty();
                if (!grant.authExpiresAt().isAfter(Instant.now())) return Mono.error(new IllegalArgumentException("Authentication expired"));
                Set<UUID> targets = requested;
                long version = subscriptionVersion;
                return users.readable(grant.userId(), targets).flatMapMany(Flux::fromIterable)
                    .flatMap(row -> store.snapshot(row.userId()).map(state -> {
                        Instant seen = state.lastSeenAt();
                        if (seen == null || row.lastSeenAt() != null && row.lastSeenAt().isAfter(seen)) seen = row.lastSeenAt();
                        return new Entry(row.userId(), state.online() ? "online" : "offline", seen);
                    }), 8).collectList().doOnNext(entries -> {
                        if (version != subscriptionVersion || !grant.authExpiresAt().isAfter(Instant.now())) return;
                        Set<UUID> removed = new HashSet<>(targets);
                        entries.forEach(entry -> removed.remove(entry.userId()));
                        send(Map.of("type", type, "epoch", connectionId, "sequence", ++sequence,
                            "entries", entries, "removed", removed));
                    }).then().onErrorResume(error -> {
                        // A failed authorization lookup must never leave a cached online status visible.
                        send(Map.of("type", "PRESENCE_REMOVED", "userIds", targets));
                        send(Map.of("type", "ERROR", "code", "PRESENCE_UNAVAILABLE"));
                        return Mono.empty();
                    });
            });
        }

        void send(Object value) { emit(new Output(false, mapper.writeValueAsString(value))); }

        synchronized void emit(Output output) {
            if (outbound.tryEmitNext(output).isFailure()) throw new IllegalStateException("Presence send queue full");
        }

        Mono<Void> cleanup() {
            outbound.tryEmitComplete();
            refreshes.tryEmitComplete();
            if (!registrationStarted || grant == null) return Mono.empty();
            return store.disconnect(grant.userId(), connectionId).then().onErrorResume(error -> Mono.empty());
        }
    }
}
