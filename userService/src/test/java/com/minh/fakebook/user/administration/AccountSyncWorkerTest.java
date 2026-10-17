package com.minh.fakebook.user.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;

class AccountSyncWorkerTest {
    @Test void retryKeepsSuspensionAndUnlockRequiresSuccessfulLogoutAndFreshToken() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:sync"+UUID.randomUUID()+";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("create table account_directory(subject varchar(36) primary key,version bigint,sync_state varchar(20),sync_attempts integer,next_attempt_at timestamp,desired_action varchar(30),archive_before timestamp,account_kind varchar(20),status varchar(30),tokens_valid_after timestamp,community_archived boolean)");
        String target=UUID.randomUUID().toString();
        jdbc.update("insert into account_directory values(?,0,'PENDING',0,?,'SUSPEND',null,'USER','SUSPENDED',?,false)",target,Timestamp.from(Instant.now().minusSeconds(1)),Timestamp.from(Instant.now()));
        var keycloak=mock(KeycloakAdmin.class);doThrow(new IllegalStateException("offline")).when(keycloak).synchronize(target,"SUSPEND");
        var worker=new AccountSyncWorker(jdbc,keycloak,mock(InternalHttp.class),mock(CommunityArchiveResource.class),new DataSourceTransactionManager(source),"post","comment","media");
        worker.retry();
        assertThat(jdbc.queryForObject("select status from account_directory",String.class)).isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("select sync_state from account_directory",String.class)).isEqualTo("RETRY");
        assertThat(jdbc.queryForObject("select sync_attempts from account_directory",Integer.class)).isEqualTo(1);
        jdbc.update("update account_directory set desired_action='UNLOCK',status='UNLOCK_PENDING',next_attempt_at=?",Timestamp.from(Instant.now().minusSeconds(1)));
        Instant old=Instant.now().minusSeconds(2);
        worker.retry();
        assertThat(jdbc.queryForObject("select status from account_directory",String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select sync_state from account_directory",String.class)).isEqualTo("SYNCED");
        var watermark=jdbc.queryForObject("select tokens_valid_after from account_directory",Timestamp.class).toInstant();
        assertThat(new AccountState(target,"USER","ACTIVE",watermark,false,1,"SYNCED").denial(old,false,false,false)).isEqualTo("TOKEN_REVOKED");
        verify(keycloak).synchronize(target,"UNLOCK");
    }
}
