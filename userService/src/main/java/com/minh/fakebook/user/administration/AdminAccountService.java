package com.minh.fakebook.user.administration;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAccountService {
    private final JdbcTemplate jdbc;
    private final AdminJournal journal;
    private final KeycloakAdmin keycloak;
    public AdminAccountService(JdbcTemplate jdbc,AdminJournal journal,KeycloakAdmin keycloak) { this.jdbc=jdbc;this.journal=journal;this.keycloak=keycloak; }
    @Transactional
    public Map<String,Object> change(String subject,String action,String key,AdminJournal.Change change) {
        var replay=journal.replay(key,subject,action,change);if (replay!=null) return replay;
        var rows=jdbc.queryForList("select * from account_directory where subject=? for update",subject);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var row=rows.getFirst();long version=((Number)row.get("version")).longValue();
        if (version!=change.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"VERSION_CONFLICT");
        if (subject.equals(journal.actor()) || ((action.equals("SUSPEND") || action.equals("UNLOCK")) && "ADMIN".equals(row.get("account_kind"))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"ADMIN_ACCOUNT_PROTECTED");
        if (!"SYNCED".equals(row.get("sync_state"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"SYNC_PENDING");
        Instant now=Instant.now();
        String status=switch(action) { case "SUSPEND" -> "SUSPENDED";case "UNLOCK" -> "UNLOCK_PENDING";default -> "ROLE_PENDING";};
        String kind=action.equals("GRANT_ADMIN")?"ADMIN":row.get("account_kind").toString();
        jdbc.update("update account_directory set status=?,account_kind=?,tokens_valid_after=?,version=version+1,sync_state='PENDING',sync_attempts=0,next_attempt_at=?,desired_action=?,archive_before=case when ? is null then archive_before else ? end,community_archived=? where subject=?",
            status,kind,Timestamp.from(now),Timestamp.from(now),action,
            action.equals("GRANT_ADMIN") || action.equals("REVOKE_ADMIN")?Timestamp.from(now):null,
            action.equals("GRANT_ADMIN") || action.equals("REVOKE_ADMIN")?Timestamp.from(now):null,
            Boolean.TRUE.equals(row.get("community_archived")) || action.equals("GRANT_ADMIN") || action.equals("REVOKE_ADMIN"),subject);
        journal.append(subject,action,change.reason(),row.get("status")+":"+row.get("account_kind"),status+":"+kind,key,change,version+1);
        return Map.of("version",version+1,"status",status,"sync_state","PENDING","replayed",false);
    }
    /** Backfill does not overwrite a suspension, role command, or archival marker. */
    @Transactional
    public int backfill() {
        int count=0;
        for(int first=0;;first+=100) {
            var users=keycloak.users(first,100);
            for(var identity:users) { provision(identity.get("id").toString());updateIdentityMetadata(identity);count++; }
            if(users.size()<100) {
                jdbc.update("update account_directory_meta set ready=true where id=1");
                return count;
            }
        }
    }
    @Transactional
    public void provision(String subject) {
        if (jdbc.queryForObject("select count(*) from account_directory where subject=?",Long.class,subject)>0) return;
        var identity=keycloak.identity(subject);
        if(identity.get("serviceAccountClientId")!=null) return;
        boolean admin=keycloak.isAdmin(subject);Instant now=Instant.now();
        try {
            jdbc.update("insert into account_directory(subject,username,account_kind,status,community_archived,archive_before,sync_state,next_attempt_at,desired_action) values(?,?,?,?,?,?,?,?,?)",
                subject,identity.getOrDefault("username",subject),admin?"ADMIN":"USER",Boolean.FALSE.equals(identity.get("enabled"))?"SUSPENDED":"ACTIVE",admin,
                admin?Timestamp.from(now):null,admin?"PENDING":"SYNCED",admin?Timestamp.from(now):null,admin?"ARCHIVE":null);
        } catch (org.springframework.dao.DuplicateKeyException concurrentProvision) { /* Another verified request provisioned the identity. */ }
        updateIdentityMetadata(identity);
    }

    /** Display data only: never overwrite account status, roles, or token revocation state. */
    @Transactional
    public void refreshIdentityMetadata() {
        for (int first=0;;first+=100) {
            var identities=keycloak.users(first,100);
            for (var identity:identities) updateIdentityMetadata(identity);
            if (identities.size()<100) return;
        }
    }
    private void updateIdentityMetadata(Map<String,Object> identity) {
        if (identity.get("serviceAccountClientId")!=null) return;
        String name=(Objects.toString(identity.get("firstName"),"")+" "+Objects.toString(identity.get("lastName"),"")).trim();
        jdbc.update("update account_directory set email=?,identity_display_name=? where subject=?",
            identity.get("email"),name.isBlank()?null:name,identity.get("id").toString());
    }
}
