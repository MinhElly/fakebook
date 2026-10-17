package com.minh.fakebook.user.administration;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Aggregate counts only. Calendar buckets always use Vietnam time, independently of the JVM zone. */
public final class AdminOverview {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public record UserStates(long active, long suspended, long processing) {}
    public record SyncStates(long synced, long pending, long retry) {}
    public record Activity(String date, long views, long changes) {}
    public record Summary(long users, long admins, long suspended, long pending, int directory_ready,
        UserStates user_states, SyncStates sync_states, int days, String timezone, List<Activity> activity) {}
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public AdminOverview(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }

    public Summary load(int days) {
        if(days!=7 && days!=30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_OVERVIEW_DAYS");
        long users=0,admins=0,active=0,suspended=0,processing=0,synced=0,pending=0,retry=0;
        for(var row:jdbc.queryForList("select account_kind,status,sync_state,count(*) as total from account_directory group by account_kind,status,sync_state")) {
            long total=((Number)row.get("total")).longValue();
            if("USER".equals(row.get("account_kind"))) {
                users+=total;
                if("ACTIVE".equals(row.get("status"))) active+=total;
                else if("SUSPENDED".equals(row.get("status"))) suspended+=total;
                else processing+=total;
            } else if("ADMIN".equals(row.get("account_kind"))) admins+=total;
            if("SYNCED".equals(row.get("sync_state"))) synced+=total;
            else if("RETRY".equals(row.get("sync_state"))) retry+=total;
            else pending+=total;
        }
        LocalDate start=LocalDate.now(clock.withZone(ZONE)).minusDays(days-1L);
        var activity=new ArrayList<Activity>();
        var arguments=new ArrayList<Object>();
        var bucket=new StringBuilder("case ");
        for(int day=0;day<days-1;day++) {
            bucket.append("when occurred_at < ? then ").append(day).append(' ');
            arguments.add(Timestamp.from(start.plusDays(day+1L).atStartOfDay(ZONE).toInstant()));
        }
        bucket.append("else ").append(days-1).append(" end");
        arguments.add(Timestamp.from(start.atStartOfDay(ZONE).toInstant()));
        arguments.add(Timestamp.from(start.plusDays(days).atStartOfDay(ZONE).toInstant()));
        String views="action in ('VIEW_ACCOUNT','VIEW_POST','VIEW_COMMENT','VIEW_MEDIA')";
        String sql="select "+bucket+" as day_index,sum(case when "+views+" then 1 else 0 end) as views,sum(case when "+views+" then 0 else 1 end) as changes from admin_audit where occurred_at>=? and occurred_at<? group by day_index";
        for(int day=0;day<days;day++) activity.add(new Activity(start.plusDays(day).toString(),0,0));
        for(var row:jdbc.queryForList(sql,arguments.toArray())) {
            int index=((Number)row.get("day_index")).intValue();
            activity.set(index,new Activity(start.plusDays(index).toString(),((Number)row.get("views")).longValue(),((Number)row.get("changes")).longValue()));
        }
        boolean ready=Boolean.TRUE.equals(jdbc.queryForObject("select ready from account_directory_meta where id=1",Boolean.class));
        // Preserve the original headline semantics: restricted Users and all unsynchronized accounts.
        return new Summary(users,admins,suspended+processing,pending+retry,ready?1:0,
            new UserStates(active,suspended,processing),new SyncStates(synced,pending,retry),days,ZONE.getId(),List.copyOf(activity));
    }
}
