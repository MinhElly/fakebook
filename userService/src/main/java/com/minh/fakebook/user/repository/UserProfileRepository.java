package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserProfile entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, JpaSpecificationExecutor<UserProfile> {
    // Metadata-only lookup: distinguish a retained archive from an identity that has never had a profile.
    @Query(value="select count(*) from user_profiles where id=:id and community_archived=true",nativeQuery=true)
    long countArchivedIdentity(@Param("id") String id);

    interface PresenceView {
        UUID getId();
        Instant getLastSeenAt();
    }

    @Query("""
        select u.id as id, u.lastSeenAt as lastSeenAt from UserProfile u
        where u.id in :targetIds and (u.id = :viewerId or exists (
            select f.id from Friendship f
            where (f.user.id = :viewerId and f.friend.id = u.id)
               or (f.friend.id = :viewerId and f.user.id = u.id)
        ))
        """)
    java.util.List<PresenceView> findReadablePresence(
        @Param("viewerId") UUID viewerId,
        @Param("targetIds") java.util.Collection<UUID> targetIds
    );
    Page<UserProfile> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String displayName, String username, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    update UserProfile u
       set u.lastSeenAt = :lastSeenAt
     where u.id = :userId
       and (
           u.lastSeenAt is null
           or u.lastSeenAt < :lastSeenAt
       )
    """)
    int updateLastSeenAtIfNewer(
        @Param("userId") UUID userId,
        @Param("lastSeenAt") Instant lastSeenAt
    );
}

