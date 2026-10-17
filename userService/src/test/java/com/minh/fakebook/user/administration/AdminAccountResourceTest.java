package com.minh.fakebook.user.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;

class AdminAccountResourceTest {
    JdbcTemplate jdbc;
    AdminJournal journal;
    AdminAccountResource resource;
    String user=UUID.randomUUID().toString(), admin=UUID.randomUUID().toString();
    Instant seen=Instant.parse("2026-10-17T03:12:00Z");

    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:admin-metadata"+user+";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("create table account_directory(subject varchar(36) primary key,username varchar(255),account_kind varchar(20),status varchar(30),community_archived boolean default false,version bigint default 0,sync_state varchar(20) default 'SYNCED',sync_attempts integer default 0,email varchar(320),identity_display_name varchar(255))");
        jdbc.execute("create table user_profiles(id varchar(36) primary key,display_name varchar(100),last_seen_at timestamp,community_archived boolean)");
        jdbc.update("insert into account_directory(subject,username,account_kind,status,email,identity_display_name) values(?,?,'USER','ACTIVE',?,?)",user,"alice","alice@example.test","Identity Alice");
        jdbc.update(connection -> {
            var statement=connection.prepareStatement("insert into user_profiles values(?,?,?,false)");
            statement.setString(1,user); statement.setString(2,"Tên hiển thị");
            statement.setTimestamp(3,Timestamp.from(seen),Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            return statement;
        });
        jdbc.update("insert into account_directory(subject,username,account_kind,status,email,identity_display_name) values(?,?,'ADMIN','ACTIVE',?,?)",admin,"admin","admin@example.test","Administrator");
        journal=mock(AdminJournal.class);
        resource=new AdminAccountResource(jdbc,mock(AdminAccountService.class),journal);
    }

    @SuppressWarnings("unchecked")
    @Test void pageIncludesProfileEmailAndLastSeenWithoutMixingAdmins() {
        var result=resource.accounts(new MockHttpServletRequest("GET","/api/admin/users"),0,1,"","");
        assertThat(result.get("total")).isEqualTo(1L);
        var rows=(List<Map<String,Object>>)result.get("items");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("display_name","Tên hiển thị").containsEntry("email","alice@example.test").containsEntry("last_seen_at",seen.toString());
        assertThat(resource.accounts(new MockHttpServletRequest("GET","/api/admin/users"),1,1,"","").get("items")).asList().isEmpty();
        for(String search:List.of("hiển thị","ALICE@EXAMPLE.TEST",user))
            assertThat(resource.accounts(new MockHttpServletRequest("GET","/api/admin/users"),0,20,search,"").get("total")).isEqualTo(1L);
    }

    @SuppressWarnings("unchecked")
    @Test void missingProfileRetainsAccountAndUnknownLastSeen() {
        var result=resource.accounts(new MockHttpServletRequest("GET","/api/admin/admins"),0,20,"","");
        var row=((List<Map<String,Object>>)result.get("items")).getFirst();
        assertThat(row).containsEntry("display_name","Administrator").containsEntry("last_seen_at",null);
    }

    @Test void detailIncludesMetadataAndMustAuditBeforeReturning() {
        var id=UUID.fromString(user);
        assertThat(resource.detail(id)).containsEntry("display_name","Tên hiển thị").containsEntry("last_seen_at",seen.toString());
        verify(journal).append(user,"VIEW_ACCOUNT","Xem tài khoản phục vụ quản trị","METADATA","DETAIL",null,null,null);
        doThrow(new IllegalStateException("audit unavailable")).when(journal).append(any(),any(),any(),any(),any(),any(),any(),any());
        assertThatThrownBy(() -> resource.detail(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test void metadataRefreshCannotReenableSuspendedAccountsOrChangeRoles() {
        jdbc.update("update account_directory set status='SUSPENDED',version=7 where subject=?",user);
        var keycloak=mock(KeycloakAdmin.class);
        when(keycloak.users(0,100)).thenReturn(List.of(Map.of("id",user,"email","new@example.test","firstName","New","lastName","Name"),Map.of("id",admin,"serviceAccountClientId","machine","email","machine@example.test")));
        new AdminAccountService(jdbc,journal,keycloak).refreshIdentityMetadata();
        assertThat(jdbc.queryForMap("select email,identity_display_name,status,account_kind,version from account_directory where subject=?",user))
            .containsEntry("email","new@example.test").containsEntry("identity_display_name","New Name").containsEntry("status","SUSPENDED").containsEntry("account_kind","USER").containsEntry("version",7L);
        assertThat(jdbc.queryForObject("select email from account_directory where subject=?",String.class,admin)).isEqualTo("admin@example.test");
    }

    @Test void scheduledRefreshCommitsWithTheProductionAutoCommitDisabledSetting() {
        try(var pool=new com.zaxxer.hikari.HikariDataSource()) {
            pool.setJdbcUrl(((DriverManagerDataSource)jdbc.getDataSource()).getUrl());
            pool.setUsername("sa"); pool.setPassword(""); pool.setAutoCommit(false);
            var keycloak=mock(KeycloakAdmin.class);
            when(keycloak.users(0,100)).thenReturn(List.of(Map.of("id",user,"email","committed@example.test")));
            var target=new AdminAccountService(new JdbcTemplate(pool),journal,keycloak);
            var factory=new org.springframework.aop.framework.ProxyFactory(target);
            factory.setProxyTargetClass(true);
            factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(pool),
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
            ((AdminAccountService)factory.getProxy()).refreshIdentityMetadata();
            // Read through a separate connection, after the scheduled method's transaction finished.
            assertThat(jdbc.queryForObject("select email from account_directory where subject=?",String.class,user)).isEqualTo("committed@example.test");
        }
    }
}
