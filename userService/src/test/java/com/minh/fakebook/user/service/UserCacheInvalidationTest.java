package com.minh.fakebook.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.minh.fakebook.user.domain.*;
import com.minh.fakebook.user.repository.*;
import java.util.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.transaction.support.*;

class UserCacheInvalidationTest {
    @Test void rejectEvictsBothUsersOnlyAfterCommit() throws Throwable {
        var requests = mock(FriendRequestRepository.class);
        var friendships = mock(FriendshipRepository.class);
        var follows = mock(FollowRepository.class);
        var redis = mock(StringRedisTemplate.class);
        var cursor = mock(Cursor.class);
        when(redis.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);
        UUID requestId = UUID.randomUUID(), senderId = UUID.randomUUID(), receiverId = UUID.randomUUID();
        var request = new FriendRequest().sender(new UserProfile().id(senderId)).receiver(new UserProfile().id(receiverId));
        when(requests.findById(requestId)).thenReturn(Optional.of(request));
        var invocation = mock(ProceedingJoinPoint.class);
        var signature = mock(Signature.class);
        when(invocation.getTarget()).thenReturn(mock(FriendRequestService.class));
        when(invocation.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("rejectFriendRequest");
        when(invocation.getArgs()).thenReturn(new Object[] {requestId, receiverId});
        TransactionSynchronizationManager.initSynchronization();
        try {
            new UserCacheInvalidation(requests, friendships, follows, redis).invalidate(invocation);
            verifyNoInteractions(redis);
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.getFirst().afterCommit();
            verify(redis).delete("userProfile::" + senderId);
            verify(redis).delete("userProfile::" + receiverId);
            verify(redis, times(10)).scan(any());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
