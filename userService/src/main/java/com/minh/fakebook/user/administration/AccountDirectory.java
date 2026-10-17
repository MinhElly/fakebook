package com.minh.fakebook.user.administration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AccountDirectory {
    private final JdbcTemplate jdbc;
    private final AdminAccountService provisioning;
    public AccountDirectory(JdbcTemplate jdbc, AdminAccountService provisioning) { this.jdbc=jdbc; this.provisioning=provisioning; }

    public AccountState state(String subject) {
        var states=jdbc.query("select * from account_directory where subject=?", AccountDirectory::map,subject);
        if (states.isEmpty()) {
            provisioning.provision(subject);
            states=jdbc.query("select * from account_directory where subject=?", AccountDirectory::map,subject);
        }
        if (states.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"ACCOUNT_NOT_PROVISIONED");
        if (!"ADMIN".equals(states.getFirst().accountKind())) requireReady();
        return states.getFirst();
    }
    static AccountState map(ResultSet r,int index) throws SQLException {
        var timestamp=r.getTimestamp("tokens_valid_after");
        return new AccountState(r.getString("subject"),r.getString("account_kind"),r.getString("status"),
            timestamp==null?null:timestamp.toInstant(),r.getBoolean("community_archived"),r.getLong("version"),r.getString("sync_state"));
    }
    public List<String> excluded() {
        requireReady();
        return jdbc.queryForList("select subject from account_directory where account_kind='ADMIN' or community_archived=true",String.class);
    }
    private void requireReady() {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select ready from account_directory_meta where id=1",Boolean.class)))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"ACCOUNT_DIRECTORY_NOT_READY");
    }
    public void requireCommunity(String subject) {
        var s=state(subject);
        if (!"USER".equals(s.accountKind()) || s.communityArchived() || !"ACTIVE".equals(s.status()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"COMMUNITY_USER_REQUIRED");
    }
}
