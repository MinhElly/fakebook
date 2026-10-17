package com.minh.fakebook.user.administration;

import static org.assertj.core.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class AdminOverviewTest {
    JdbcTemplate jdbc;
    Clock clock=Clock.fixed(Instant.parse("2026-10-18T02:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:overview"+UUID.randomUUID()+";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("create table account_directory(account_kind varchar(20),status varchar(30),sync_state varchar(20))");
        jdbc.execute("create table account_directory_meta(id int,ready boolean)");
        jdbc.update("insert into account_directory_meta values(1,true)");
        jdbc.execute("create table admin_audit(event_id varchar(36) primary key,action varchar(50),occurred_at timestamp)");
    }
    void account(String kind,String status,String sync) {jdbc.update("insert into account_directory values(?,?,?)",kind,status,sync);}
    void event(String when,String action) {jdbc.update("insert into admin_audit values(?,?,?)",UUID.randomUUID().toString(),action,Timestamp.from(Instant.parse(when)));}
    @Test void aggregatesUsersSeparatelyAndAllAccountSynchronization() {
        account("USER","ACTIVE","SYNCED");account("USER","ACTIVE","PENDING");
        account("USER","SUSPENDED","RETRY");account("USER","UNLOCK_PENDING","SYNCED");account("ADMIN","ACTIVE","SYNCED");
        var result=new AdminOverview(jdbc,clock).load(7);
        assertThat(result.users()).isEqualTo(4);assertThat(result.admins()).isEqualTo(1);
        assertThat(result.suspended()).isEqualTo(2);assertThat(result.pending()).isEqualTo(2);
        assertThat(result.user_states()).isEqualTo(new AdminOverview.UserStates(2,1,1));
        assertThat(result.sync_states()).isEqualTo(new AdminOverview.SyncStates(3,1,1));
        assertThat(result.directory_ready()).isEqualTo(1);
    }
    @Test void vietnamMidnightAndDateRolloverUseHalfOpenRanges() {
        event("2026-10-11T16:59:59Z","VIEW_ACCOUNT"); // Before the 7-day range.
        event("2026-10-11T17:00:00Z","VIEW_POST"); // 12 October, 00:00 GMT+7.
        event("2026-10-17T16:59:59Z","VIEW_MEDIA");
        event("2026-10-17T17:00:00Z","SUSPEND");
        event("2026-10-17T18:30:00Z","VIEW_COMMENT"); // 18 October, 01:30 GMT+7.
        event("2026-10-18T17:00:00Z","UNLOCK"); // Next day, excluded.
        var result=new AdminOverview(jdbc,clock).load(7);
        assertThat(result.timezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(result.activity()).hasSize(7);
        assertThat(result.activity().getFirst()).isEqualTo(new AdminOverview.Activity("2026-10-12",1,0));
        assertThat(result.activity().get(1)).isEqualTo(new AdminOverview.Activity("2026-10-13",0,0));
        assertThat(result.activity().get(5)).isEqualTo(new AdminOverview.Activity("2026-10-17",1,0));
        assertThat(result.activity().getLast()).isEqualTo(new AdminOverview.Activity("2026-10-18",1,1));
    }
    @Test void thirtyDaysIncludeZeroDaysAndAllViewKinds() {
        for(String action:new String[]{"VIEW_ACCOUNT","VIEW_POST","VIEW_COMMENT","VIEW_MEDIA"}) event("2026-10-17T18:30:00Z",action);
        for(String action:new String[]{"HIDE","RESTORE","GRANT_ADMIN","REVOKE_ADMIN","UNLOCK"}) event("2026-10-17T19:00:00Z",action);
        var result=new AdminOverview(jdbc,clock).load(30);
        assertThat(result.activity()).hasSize(30);
        assertThat(result.activity().getFirst()).isEqualTo(new AdminOverview.Activity("2026-09-19",0,0));
        assertThat(result.activity().getLast()).isEqualTo(new AdminOverview.Activity("2026-10-18",4,5));
    }
    @Test void emptyDataAndInvalidPeriod() {
        assertThat(new AdminOverview(jdbc,clock).load(7).activity()).allMatch(day -> day.views()==0 && day.changes()==0);
        assertThatThrownBy(() -> new AdminOverview(jdbc,clock).load(14)).isInstanceOfSatisfying(ResponseStatusException.class,error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
    }
}
