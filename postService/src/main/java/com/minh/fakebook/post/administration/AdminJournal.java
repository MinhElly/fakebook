package com.minh.fakebook.post.administration;

import java.time.Instant;
import java.util.*;
import io.namastack.outbox.Outbox;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AdminJournal {
    private final JdbcTemplate jdbc;
    private final Outbox outbox;
    public AdminJournal(JdbcTemplate jdbc, Outbox outbox) { this.jdbc=jdbc; this.outbox=outbox; }
    public record Change(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=1000) String reason,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long version) {}
    public String actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"ADMIN_REQUIRED");
        return jwt.getToken().getSubject();
    }
    public Map<String,Object> replay(String key,String target,String action,Change change) {
        if (key==null || key.isBlank() || key.length()>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_IDEMPOTENCY_KEY");
        var prior=jdbc.queryForList("select fingerprint,result_version from admin_audit where actor=? and idempotency_key=?",actor(),key);
        if (prior.isEmpty()) return null;
        if (!fingerprint(target,action,change).equals(prior.getFirst().get("fingerprint")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT");
        return Map.of("version",prior.getFirst().get("result_version"),"replayed",true);
    }
    private String fingerprint(String target,String action,Change change) {
        return target+"|"+action+"|"+change.version()+"|"+change.reason();
    }
    public void append(String target,String action,String reason,String before,String after,String key,Change change,Long version) {
        String requestId=UUID.randomUUID().toString();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request) {
            String supplied=request.getRequest().getHeader("X-Request-ID");
            if (supplied!=null && supplied.matches("[A-Za-z0-9._:-]{1,100}")) requestId=supplied;
        }
        var event=new AdminAuditEvent(UUID.randomUUID().toString(),actor(),target,action,reason,Instant.now(),before,after,requestId);
        jdbc.update("insert into admin_audit(event_id,actor,target,action,reason,occurred_at,before_state,after_state,request_id,idempotency_key,fingerprint,result_version) values(?,?,?,?,?,?,?,?,?,?,?,?)",
            event.eventId(),event.actor(),target,action,reason,java.sql.Timestamp.from(event.occurredAt()),before,after,requestId,key,
            change==null?null:fingerprint(target,action,change),version);
        outbox.schedule(event,"admin-audit-"+target,Map.of("destination","admin-audit-events"));
    }
}
