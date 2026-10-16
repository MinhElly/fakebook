package com.minh.fakebook.gateway.presence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PresenceRedisStore {

    private static final Duration CONNECTION_LEASE =
        Duration.ofSeconds(65);

    private static final String STATE_SCRIPT_PREFIX = """
            -- A Lua runtime error does not roll back earlier writes.
            -- Validate all key types and metadata before changing connections.
            local expectedTypes = {'zset', 'hash', 'zset', 'hash', 'zset'}
            for index, expectedType in ipairs(expectedTypes) do
                local actualType = redis.call('TYPE', KEYS[index]).ok
                if actualType ~= 'none' and actualType ~= expectedType then
                    return redis.error_reply(
                        'WRONGTYPE Presence key ' .. index
                        .. ' must be ' .. expectedType
                    )
                end
            end

            local previousValue = redis.call(
                'HGET', KEYS[2], 'lastSeenAt'
            )
            local previousLastSeen = 0
            if previousValue then
                previousLastSeen = tonumber(previousValue)
                if not previousLastSeen
                    or previousLastSeen ~= previousLastSeen
                    or previousLastSeen == math.huge
                    or previousLastSeen == -math.huge then
                    return redis.error_reply(
                        'ERR Invalid presence lastSeenAt'
                    )
                end
            end

            local time = redis.call('TIME')
            local now = tonumber(time[1]) * 1000
                + math.floor(tonumber(time[2]) / 1000)
            local persisted = tonumber(redis.call('HGET', KEYS[2], 'persistedAt') or '0')
            if not persisted or persisted ~= persisted or persisted == math.huge or persisted == -math.huge then
                return redis.error_reply('ERR Invalid presence persistedAt')
            end
            """;

    private static final RedisScript<Long> CONNECTION_SCRIPT =
        new DefaultRedisScript<>(
            STATE_SCRIPT_PREFIX + """
            local connectionId = ARGV[1]
            local leaseMillis = tonumber(ARGV[2])
            local userId = ARGV[3]
            local expiresAt = now + leaseMillis
            local wasOnline = redis.call('ZCOUNT', KEYS[1], '(' .. now, '+inf') > 0

            -- A late PONG must not recreate a closed or expired connection.
            if ARGV[4] == '1' then
                local currentDeadline = redis.call(
                    'ZSCORE', KEYS[1], connectionId
                )
                if not currentDeadline
                    or tonumber(currentDeadline) <= now then
                    return 0
                end
            end

            redis.call(
                'ZREMRANGEBYSCORE',
                KEYS[1],
                '-inf',
                now
            )

            redis.call(
                'ZADD',
                KEYS[1],
                expiresAt,
                connectionId
            )

            if now > previousLastSeen then
                redis.call(
                    'HSET',
                    KEYS[2],
                    'lastSeenAt',
                    now
                )
            end
            redis.call('PERSIST', KEYS[2])

            -- Queue the latest observation atomically with the lease mutation.
            -- Due dates coalesce PONGs into one database checkpoint per minute.
            redis.call('HSET', KEYS[4], userId, math.max(now, previousLastSeen))
            redis.call('ZADD', KEYS[5], 'NX', math.max(now, persisted + 60000), userId)
            if not wasOnline then redis.call('PUBLISH', ARGV[5], userId) end

            local firstConnection = redis.call(
                'ZRANGE',
                KEYS[1],
                0,
                0,
                'WITHSCORES'
            )

            redis.call(
                'ZADD',
                KEYS[3],
                firstConnection[2],
                userId
            )

            return redis.call('ZCARD', KEYS[1])
            """,
            Long.class
        );

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> CLEANUP_SCRIPT =
        new DefaultRedisScript<>(
            STATE_SCRIPT_PREFIX + """
            local removed = 0
            if ARGV[1] ~= '' then
                removed = redis.call('ZREM', KEYS[1], ARGV[1])
            end
            local expired = redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now)
            local remaining = redis.call('ZCARD', KEYS[1])

            if remaining > 0 then
                local firstConnection = redis.call(
                    'ZRANGE', KEYS[1], 0, 0, 'WITHSCORES'
                )
                redis.call('ZADD', KEYS[3], firstConnection[2], ARGV[2])
            else
                redis.call('DEL', KEYS[1])
                redis.call('ZREM', KEYS[3], ARGV[2])
                if previousValue then
                    redis.call('HSET', KEYS[4], ARGV[2], previousValue)
                    redis.call('ZADD', KEYS[5], now, ARGV[2])
                end
                if removed + expired > 0 then redis.call('PUBLISH', ARGV[3], ARGV[2]) end
            end

            -- Keep the last observed time for persistence and retry.
            -- Closing a socket must not advance lastSeenAt to cleanup time.
            return {
                remaining,
                removed,
                previousLastSeen,
                previousValue and 1 or 0,
                expired
            }
            """,
            List.class
        );

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> DUE_USERS_SCRIPT = new DefaultRedisScript<>(
        """
        local actualType = redis.call('TYPE', KEYS[1]).ok
        if actualType ~= 'none' and actualType ~= 'zset' then
            return redis.error_reply('WRONGTYPE Presence due index must be zset')
        end
        local time = redis.call('TIME')
        local now = tonumber(time[1]) * 1000
            + math.floor(tonumber(time[2]) / 1000)
        return redis.call(
            'ZRANGEBYSCORE', KEYS[1], '-inf', now,
            'LIMIT', 0, tonumber(ARGV[1])
        )
        """,
        List.class
    );

    private final ReactiveStringRedisTemplate redis;
    private final String keyPrefix;
    private final String dueKey;

    public PresenceRedisStore(
        ReactiveStringRedisTemplate redis,
        @Value("${presence.redis-key-prefix:fakebook:presence}") String keyPrefix
    ) {
        this.redis = redis;
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("Presence Redis key prefix must not be blank");
        }
        this.keyPrefix = keyPrefix;
        this.dueKey = keyPrefix + ":due";
    }

    /**
     * Registers a server-generated connection ID after WebSocket authentication.
     * Returns the number of unexpired connections at registration time.
     * Connection deadlines, rather than a whole-key TTL, drive later cleanup.
     */
    public Mono<Long> register(
        UUID userId,
        UUID connectionId
    ) {
        return updateConnection(userId, connectionId, false);
    }

    /**
     * Renews a live connection after receiving a valid WebSocket PONG.
     * False means this connection is no longer live, not that the user is offline.
     */
    public Mono<Boolean> heartbeat(
        UUID userId,
        UUID connectionId
    ) {
        return updateConnection(userId, connectionId, true)
            .map(count -> count > 0L);
    }

    /**
     * Snapshot taken atomically when removing a connection. lastSeenAt is null
     * when Redis has no observation for this user. A missing connection is a no-op
     * apart from pruning expired connections and reconciling the due index.
     */
    public record DisconnectResult(long remainingConnections, boolean removed, Instant lastSeenAt) {}

    /**
     * Removes only the supplied connection. Metadata is retained so persistence
     * can be retried after the final connection closes.
     */
    public Mono<DisconnectResult> disconnect(UUID userId, UUID connectionId) {
        return Mono.defer(() -> {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(connectionId, "connectionId must not be null");

            return redis.execute(
                CLEANUP_SCRIPT,
                keys(userId),
                List.of(connectionId.toString(), userId.toString(), channel())
            ).single().map(PresenceRedisStore::toDisconnectResult);
        });
    }

    /** Reads a bounded batch without removing index entries before cleanup succeeds. */
    public Flux<UUID> findDueUsers(int size) {
        return Flux.defer(() -> {
            if (size < 1 || size > 1_000) {
                return Flux.error(new IllegalArgumentException("Presence expiry batch size must be between 1 and 1000"));
            }
            return redis.execute(DUE_USERS_SCRIPT, List.of(dueKey), List.of(Integer.toString(size)))
                .single()
                .flatMapIterable(PresenceRedisStore::toUserIds);
        });
    }

    public record ExpiryResult(long remainingConnections, long expiredConnections, Instant lastSeenAt) {}

    /** Rechecks deadlines in Redis, including heartbeats received after the batch was read. */
    public Mono<ExpiryResult> pruneExpired(UUID userId) {
        return Mono.defer(() -> {
            Objects.requireNonNull(userId, "userId must not be null");
            return redis.execute(CLEANUP_SCRIPT, keys(userId), List.of("", userId.toString(), channel()))
                .single()
                .map(values -> {
                    DisconnectResult state = toDisconnectResult(values);
                    return new ExpiryResult(
                        state.remainingConnections(),
                        ((Number) values.get(4)).longValue(),
                        state.lastSeenAt()
                    );
                });
        });
    }

    private static List<UUID> toUserIds(List<?> values) {
        return values.stream().map(value -> UUID.fromString(value.toString())).toList();
    }

    private static DisconnectResult toDisconnectResult(List<?> values) {
        long remaining = ((Number) values.get(0)).longValue();
        boolean removed = ((Number) values.get(1)).longValue() == 1L;
        boolean hasLastSeen = ((Number) values.get(3)).longValue() == 1L;
        Instant lastSeenAt = hasLastSeen ? Instant.ofEpochMilli(((Number) values.get(2)).longValue()) : null;
        return new DisconnectResult(remaining, removed, lastSeenAt);
    }

    private List<String> keys(UUID userId) {
        String prefix = keyPrefix + ":user:" + userId;
        return List.of(prefix + ":connections", prefix + ":metadata", dueKey, keyPrefix + ":pending", keyPrefix + ":persist-due");
    }

    private Mono<Long> updateConnection(
        UUID userId,
        UUID connectionId,
        boolean requireExisting
    ) {
        return Mono.defer(() -> {
            Objects.requireNonNull(
                userId,
                "userId must not be null"
            );
            Objects.requireNonNull(
                connectionId,
                "connectionId must not be null"
            );

            List<String> arguments = List.of(
                connectionId.toString(),
                Long.toString(CONNECTION_LEASE.toMillis()),
                userId.toString(),
                requireExisting ? "1" : "0",
                channel()
            );

            return redis
                .execute(
                    CONNECTION_SCRIPT,
                    keys(userId),
                    arguments
                )
                .single();
        });
    }

    public String channel() { return keyPrefix + ":changes"; }

    public record Snapshot(boolean online, Instant lastSeenAt) {}

    private static final RedisScript<List> SNAPSHOT = new DefaultRedisScript<>("""
        local t = redis.call('TIME')
        local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
        return {redis.call('ZCOUNT', KEYS[1], '(' .. now, '+inf'),
                redis.call('HGET', KEYS[2], 'lastSeenAt') or ''}
        """, List.class);

    public Mono<Snapshot> snapshot(UUID userId) {
        return redis.execute(SNAPSHOT, keys(userId).subList(0, 2)).single().map(values -> {
            String seen = values.get(1).toString();
            return new Snapshot(((Number) values.get(0)).longValue() > 0,
                seen.isEmpty() ? null : Instant.ofEpochMilli(Long.parseLong(seen)));
        });
    }

    public Flux<UUID> findPending(int size) {
        return redis.execute(DUE_USERS_SCRIPT, List.of(keyPrefix + ":persist-due"), List.of(Integer.toString(size)))
            .single().flatMapIterable(PresenceRedisStore::toUserIds);
    }

    /** The lease prevents two Gateways writing the same checkpoint concurrently. */
    public Mono<Instant> claimPending(UUID userId, String owner) {
        return redis.opsForValue().setIfAbsent(keyPrefix + ":persist-lock:" + userId, owner, Duration.ofSeconds(20))
            .flatMap(claimed -> claimed ? redis.<String, String>opsForHash().get(keyPrefix + ":pending", userId.toString()) : Mono.empty())
            .map(value -> Instant.ofEpochMilli(Long.parseLong(value)));
    }

    private static final RedisScript<Long> ACK = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        local t = redis.call('TIME')
        local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
        if ARGV[4] == '1' then
            redis.call('HSET', KEYS[4], 'persistedAt', now)
            if redis.call('HGET', KEYS[2], ARGV[2]) == ARGV[3] then
                redis.call('HDEL', KEYS[2], ARGV[2])
                redis.call('ZREM', KEYS[3], ARGV[2])
                -- Offline metadata is safe to expire only after the DB write succeeded.
                if redis.call('ZCARD', KEYS[5]) == 0 then redis.call('EXPIRE', KEYS[4], 86400) end
            else
                local online = redis.call('ZCOUNT', KEYS[5], '(' .. now, '+inf') > 0
                redis.call('ZADD', KEYS[3], online and now + 60000 or now, ARGV[2])
            end
        else
            redis.call('ZADD', KEYS[3], now + 5000, ARGV[2])
        end
        redis.call('DEL', KEYS[1])
        return 1
        """, Long.class);

    public Mono<Void> finishPending(UUID userId, String owner, Instant observation, boolean success) {
        return redis.execute(ACK, List.of(keyPrefix + ":persist-lock:" + userId, keyPrefix + ":pending",
            keyPrefix + ":persist-due", keys(userId).get(1), keys(userId).get(0)),
            List.of(owner, userId.toString(), Long.toString(observation.toEpochMilli()), success ? "1" : "0")).then();
    }
}
