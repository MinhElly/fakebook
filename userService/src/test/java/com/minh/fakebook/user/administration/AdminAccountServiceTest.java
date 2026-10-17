package com.minh.fakebook.user.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import io.namastack.outbox.Outbox;

class AdminAccountServiceTest {
    JdbcTemplate jdbc;
    AdminAccountService service;
    String actor=UUID.randomUUID().toString(),target=UUID.randomUUID().toString();
    @BeforeEach void setup() throws Exception {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:admin"+UUID.randomUUID()+";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
        String xml=new String(getClass().getResourceAsStream("/config/liquibase/changelog/20261017000000_administration.xml").readAllBytes(),StandardCharsets.UTF_8);
        String sql=xml.substring(xml.indexOf("<![CDATA[")+9,xml.indexOf("]]>"));
        for(String statement:sql.split(";")) if(!statement.isBlank() && !statement.strip().startsWith("ALTER")) jdbc.execute(statement);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("verified").header("alg","RS256").subject(actor).build(),List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        jdbc.update("insert into account_directory(subject,username,account_kind,status) values(?,?,?,?)",target,"user","USER","ACTIVE");
        service=new AdminAccountService(jdbc,new AdminJournal(jdbc,mock(Outbox.class)),mock(KeycloakAdmin.class));
    }
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}
    @Test void suspensionAndAuditAreImmediateAndReplayIsStable() {
        var request=new AdminJournal.Change("Vi phạm đã xác minh",0L);
        service.change(target,"SUSPEND","op-1",request);
        assertThat(jdbc.queryForObject("select status from account_directory where subject=?",String.class,target)).isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("select count(*) from admin_audit",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select tokens_valid_after from account_directory where subject=?",java.sql.Timestamp.class,target)).isNotNull();
        assertThat(service.change(target,"SUSPEND","op-1",request)).containsEntry("replayed",true);
        assertThatThrownBy(()->service.change(target,"UNLOCK","op-1",request)).isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("select count(*) from admin_audit",Long.class)).isEqualTo(1);
    }
    @Test void refusesStaleVersionsSelfAndOtherAdmins() {
        assertThatThrownBy(()->service.change(target,"SUSPEND","stale",new AdminJournal.Change("reason",9L))).isInstanceOf(ResponseStatusException.class);
        jdbc.update("update account_directory set account_kind='ADMIN' where subject=?",target);
        assertThatThrownBy(()->service.change(target,"SUSPEND","admin",new AdminJournal.Change("reason",0L))).isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("select count(*) from admin_audit",Long.class)).isZero();
    }
    @Test void unlockingStaysBlockedUntilSyncAndRoleChangesArchiveWithoutDeleting() {
        jdbc.update("update account_directory set status='SUSPENDED' where subject=?",target);
        service.change(target,"UNLOCK","unlock",new AdminJournal.Change("Đã xác minh",0L));
        assertThat(jdbc.queryForObject("select status from account_directory where subject=?",String.class,target)).isEqualTo("UNLOCK_PENDING");
        assertThatThrownBy(()->service.change(target,"GRANT_ADMIN","grant",new AdminJournal.Change("reason",1L))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void communityStaysClosedUntilInitialBackfillCompletesButAdminCanBootstrap() {
        var directory=new AccountDirectory(jdbc,mock(AdminAccountService.class));
        assertThatThrownBy(()->directory.state(target)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(directory::excluded).isInstanceOf(ResponseStatusException.class);
        jdbc.update("insert into account_directory(subject,username,account_kind,status) values(?,?,'ADMIN','ACTIVE')",actor,"admin");
        assertThat(directory.state(actor).accountKind()).isEqualTo("ADMIN");
        var keycloak=mock(KeycloakAdmin.class);when(keycloak.users(0,100)).thenReturn(List.of());
        new AdminAccountService(jdbc,new AdminJournal(jdbc,mock(Outbox.class)),keycloak).backfill();
        assertThat(directory.state(target).accountKind()).isEqualTo("USER");
        assertThat(directory.excluded()).containsExactly(actor);
    }
}
