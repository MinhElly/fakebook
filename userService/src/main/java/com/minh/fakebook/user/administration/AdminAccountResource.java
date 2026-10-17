package com.minh.fakebook.user.administration;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN') and !hasAuthority('ROLE_INTERNAL')")
public class AdminAccountResource {
    private final JdbcTemplate jdbc;
    private final AdminAccountService service;
    private final AdminJournal journal;
    public AdminAccountResource(JdbcTemplate jdbc,AdminAccountService service,AdminJournal journal) {this.jdbc=jdbc;this.service=service;this.journal=journal;}
    @GetMapping("/overview")
    public AdminOverview.Summary overview(@RequestParam(defaultValue="7") int days) {
        return new AdminOverview(jdbc,java.time.Clock.systemUTC()).load(days);
    }
    @GetMapping({"/users","/admins"})
    public Map<String,Object> accounts(jakarta.servlet.http.HttpServletRequest request,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status) {
        page=Math.max(0,page);size=Math.clamp(size,1,100);
        String kind=request.getRequestURI().endsWith("/admins")?"ADMIN":"USER";
        String search="%"+q.toLowerCase(Locale.ROOT)+"%";
        String where=" where a.account_kind=? and (lower(a.username) like ? or a.subject like ? or lower(coalesce(p.display_name,a.identity_display_name,'')) like ? or lower(a.email) like ?)";
        List<Object> args=new ArrayList<>(List.of(kind,search,search,search,search));
        if(!status.isBlank()) {where+=" and a.status=?";args.add(status);}
        String from=" from account_directory a left join user_profiles p on p.id=a.subject";
        long total=jdbc.queryForObject("select count(*)"+from+where,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        return Map.of("items",accountRows("select a.subject,a.username,a.account_kind,a.status,a.community_archived,a.version,a.sync_state,a.sync_attempts,a.email,coalesce(nullif(p.display_name,''),a.identity_display_name,a.username) as display_name,p.last_seen_at"+from+where+" order by a.username,a.subject limit ? offset ?",args.toArray()),"total",total,"page",page,"size",size);
    }
    @GetMapping("/users/{id}")
    @Transactional
    public Map<String,Object> detail(@PathVariable UUID id) {
        var rows=accountRows("select a.subject,a.username,a.account_kind,a.status,a.version,a.sync_state,a.sync_attempts,a.email,coalesce(nullif(p.display_name,''),a.identity_display_name,a.username) as display_name,p.last_seen_at from account_directory a left join user_profiles p on p.id=a.subject where a.subject=?",id.toString());
        if(rows.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        journal.append(id.toString(),"VIEW_ACCOUNT","Xem tài khoản phục vụ quản trị","METADATA","DETAIL",null,null,null);
        return rows.getFirst();
    }
    private List<Map<String,Object>> accountRows(String sql,Object... args) {
        return jdbc.query(sql,(result,index) -> {
            var row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(result,index);
            // Presence is written by Hibernate with hibernate.jdbc.time_zone=UTC.
            var seen=result.getTimestamp("last_seen_at",Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            row.put("last_seen_at",seen==null?null:seen.toInstant().toString());
            return row;
        },args);
    }
    @PostMapping("/users/{id}/{action:suspend|unlock}")
    public Map<String,Object> change(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AdminJournal.Change change) {
        return service.change(id.toString(),action.equals("suspend")?"SUSPEND":"UNLOCK",key,change);
    }
    @PostMapping("/account-commands/{id}/{action:grant-admin|revoke-admin}")
    public Map<String,Object> role(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AdminJournal.Change change) {
        return service.change(id.toString(),action.equals("grant-admin")?"GRANT_ADMIN":"REVOKE_ADMIN",key,change);
    }
    @PostMapping("/account-commands/backfill")
    public Map<String,Object> backfill() { return Map.of("processed",service.backfill()); }
    @GetMapping("/audit")
    public Map<String,Object> audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(defaultValue="") String actor,@RequestParam(defaultValue="") String target,@RequestParam(defaultValue="") String action,
        @RequestParam(required=false) java.time.Instant from,@RequestParam(required=false) java.time.Instant to) {
        page=Math.max(0,page);size=Math.clamp(size,1,100);
        String where=" where actor like ? and target like ? and action like ?";
        List<Object> args=new ArrayList<>(List.of("%"+actor+"%","%"+target+"%","%"+action+"%"));
        if(from!=null) {where+=" and occurred_at>=?";args.add(java.sql.Timestamp.from(from));}
        if(to!=null) {where+=" and occurred_at<=?";args.add(java.sql.Timestamp.from(to));}
        long total=jdbc.queryForObject("select count(*) from admin_audit"+where,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        return Map.of("items",jdbc.queryForList("select event_id,actor,target,action,reason,occurred_at,before_state,after_state,request_id from admin_audit"+where+" order by occurred_at desc,event_id desc limit ? offset ?",args.toArray()),"total",total,"page",page,"size",size);
    }
}
