package com.minh.fakebook.user.administration;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class AccountSyncWorker {
    private final JdbcTemplate jdbc;
    private final KeycloakAdmin keycloak;
    private final InternalHttp http;
    private final CommunityArchiveResource localArchive;
    private final TransactionTemplate transaction;
    private final List<String> peers;
    public AccountSyncWorker(JdbcTemplate jdbc,KeycloakAdmin keycloak,InternalHttp http,CommunityArchiveResource localArchive,
        PlatformTransactionManager transactions,
        @Value("${POST_SERVICE_URL:http://localhost:8083}") String posts,
        @Value("${COMMENT_SERVICE_URL:http://localhost:8085}") String comments,
        @Value("${MEDIA_SERVICE_URL:http://localhost:8084}") String media) {
        this.jdbc=jdbc;this.keycloak=keycloak;this.http=http;this.localArchive=localArchive;
        this.transaction=new TransactionTemplate(transactions);this.peers=List.of(posts,comments,media);
    }
    @Scheduled(fixedDelayString="${administration.sync-interval:5000}")
    public void retry() {
        var pending=jdbc.queryForList("select subject,version from account_directory where sync_state<>'SYNCED' and next_attempt_at<=? order by next_attempt_at limit 20",Timestamp.from(Instant.now()));
        for(var candidate:pending) synchronize(candidate.get("subject").toString(),((Number)candidate.get("version")).longValue());
    }
    public void synchronize(String subject,long version) {
        // Claim with a lease, so multiple pods do not run the same synchronization concurrently.
        int claimed=jdbc.update("update account_directory set next_attempt_at=? where subject=? and version=? and sync_state<>'SYNCED' and next_attempt_at<=?",
            Timestamp.from(Instant.now().plusSeconds(120)),subject,version,Timestamp.from(Instant.now()));
        if(claimed==0) return;
        var row=jdbc.queryForMap("select * from account_directory where subject=?",subject);
        String action=Objects.toString(row.get("desired_action"),"ARCHIVE");
        try {
            if(row.get("archive_before")!=null && (action.equals("ARCHIVE") || action.equals("GRANT_ADMIN") || action.equals("REVOKE_ADMIN"))) {
                var before=((Timestamp)row.get("archive_before")).toInstant();
                // The local archive endpoint has a service-only guard; call its SQL through a local transaction.
                transaction.executeWithoutResult(ignored -> archiveLocal(subject,before));
                for(String base:peers) http.client().post().uri(base+"/api/internal/accounts/{id}/archive",subject)
                    .headers(h -> h.setBearerAuth(http.token())).body(Map.of("before",before)).retrieve().toBodilessEntity();
            }
            if(!action.equals("ARCHIVE")) keycloak.synchronize(subject,action);
            transaction.executeWithoutResult(ignored -> {
                String kind=action.equals("REVOKE_ADMIN")?"USER":row.get("account_kind").toString();
                String status=action.equals("SUSPEND")?"SUSPENDED":action.equals("ARCHIVE")?row.get("status").toString():"ACTIVE";
                jdbc.update("update account_directory set account_kind=?,status=?,sync_state='SYNCED',desired_action=null,next_attempt_at=null,version=version+1,tokens_valid_after=case when ?='ARCHIVE' then tokens_valid_after else ? end,community_archived=false where subject=? and version=?",
                    kind,status,action,Timestamp.from(Instant.now()),subject,version);
            });
        } catch(Exception failure) {
            int attempts=((Number)row.get("sync_attempts")).intValue()+1;
            long delay=Math.min(3600,5L << Math.min(attempts,9));
            jdbc.update("update account_directory set sync_state='RETRY',sync_attempts=?,next_attempt_at=? where subject=? and version=?",
                attempts,Timestamp.from(Instant.now().plusSeconds(delay)),subject,version);
            // Do not log upstream response bodies: they may include identity information.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Account synchronization pending for {} (attempt {})",subject,attempts);
        }
    }
    private void archiveLocal(String subject,Instant before) {
        jdbc.update("update user_profiles set community_archived=true where id=? and created_at<=?",subject,Timestamp.from(before));
        jdbc.update("update friendships set community_archived=true where user_id=? or friend_id=?",subject,subject);
        jdbc.update("update follows set community_archived=true where follower_id=? or following_id=?",subject,subject);
        jdbc.update("update friend_requests set community_archived=true where sender_id=? or receiver_id=?",subject,subject);
    }
}
