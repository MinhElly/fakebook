package com.minh.fakebook.user.service;

import com.minh.fakebook.user.repository.*;
import java.util.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** Invalidates every page/sort for affected users only, after the database commits. */
@Aspect
@Component
@Order(1)
public class UserCacheInvalidation {
    private final FriendRequestRepository requests;
    private final FriendshipRepository friendships;
    private final FollowRepository follows;
    private final StringRedisTemplate redis;

    public UserCacheInvalidation(FriendRequestRepository requests, FriendshipRepository friendships,
        FollowRepository follows, StringRedisTemplate redis) {
        this.requests = requests;
        this.friendships = friendships;
        this.follows = follows;
        this.redis = redis;
    }

    @Around("(execution(* com.minh.fakebook.user.service.FriendRequestService.*(..)) || " +
        "execution(* com.minh.fakebook.user.service.FriendshipService.*(..)) || " +
        "execution(* com.minh.fakebook.user.service.FollowService.*(..)) || " +
        "execution(* com.minh.fakebook.user.service.UserProfileService.*(..))) && " +
        "(execution(* *.save(..)) || execution(* *.update(..)) || execution(* *.partialUpdate(..)) || " +
        "execution(* *.delete(..)) || execution(* *.sendFriendRequest(..)) || execution(* *.acceptFriendRequest(..)) || " +
        "execution(* *.rejectFriendRequest(..)) || execution(* *.cancelFriendRequest(..)) || " +
        "execution(* *.unFriend(..)) || execution(* *.followUser(..)) || execution(* *.unfollowUser(..)))")
    public Object invalidate(ProceedingJoinPoint invocation) throws Throwable {
        String service = invocation.getTarget().getClass().getSimpleName();
        String method = invocation.getSignature().getName();
        Set<UUID> users = new HashSet<>();
        Object[] args = invocation.getArgs();
        boolean profile = service.equals("UserProfileService");
        boolean crud = Set.of("save", "update", "partialUpdate", "delete").contains(method);
        if (crud) {
            UUID entityId = args[0] instanceof UUID uuid ? uuid : id(args[0], "id");
            if (profile) {
                if (entityId != null) {
                    users.add(entityId);
                    users.addAll(follows.findRelatedUserIds(entityId));
                    users.addAll(friendships.findRelatedUserIds(entityId));
                    users.addAll(requests.findRelatedUserIds(entityId));
                }
            } else if (entityId != null) {
                Object old = service.equals("FriendRequestService") ? requests.findById(entityId).orElse(null)
                    : service.equals("FriendshipService") ? friendships.findById(entityId).orElse(null)
                    : follows.findById(entityId).orElse(null);
                collect(old, users);
            }
            collect(args[0], users);
        } else if (service.equals("FriendRequestService") && !method.equals("sendFriendRequest")) {
            collect(requests.findById((UUID) args[0]).orElse(null), users);
        } else {
            for (Object arg : args) if (arg instanceof UUID uuid) users.add(uuid);
        }
        Object result = invocation.proceed();
        Object value = result instanceof Optional<?> optional ? optional.orElse(null) : result;
        collect(value, users);
        if (profile) {
            UUID entityId = id(value, "id");
            if (entityId != null) users.add(entityId);
        }
        Runnable eviction = () -> {
            for (UUID user : users) {
                for (String cache : List.of("pendingSentRequests", "pendingReceivedRequests", "userFriends", "userFollowing", "userFollowers")) {
                    try (var keys = redis.scan(ScanOptions.scanOptions().match(cache + "::" + user + "_*").count(100).build())) {
                        while (keys.hasNext()) redis.delete(keys.next());
                    }
                }
                redis.delete("userProfile::" + user);
                redis.delete("userProfile::" + user + ":optional");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { eviction.run(); }
            });
        } else eviction.run();
        return result;
    }

    private void collect(Object value, Set<UUID> users) {
        if (value == null || value instanceof UUID) return;
        for (String relationship : List.of("sender", "receiver", "user", "friend", "follower", "following")) {
            UUID entityId = id(value, relationship + ".id");
            if (entityId != null) users.add(entityId);
        }
    }

    private UUID id(Object value, String path) {
        if (value == null) return null;
        try {
            Object entityId = new BeanWrapperImpl(value).getPropertyValue(path);
            return entityId instanceof UUID uuid ? uuid : null;
        } catch (org.springframework.beans.BeansException ignored) { return null; }
    }
}
