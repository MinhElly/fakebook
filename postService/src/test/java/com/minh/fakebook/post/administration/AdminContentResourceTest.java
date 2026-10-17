package com.minh.fakebook.post.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.minh.fakebook.post.web.rest.AdminContentResource;
import io.namastack.outbox.Outbox;

class AdminContentResourceTest {
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}
    @Test void metadataDoesNotExposeContentAndAuditOutboxFailureRollsBackMutation() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:contentadmin"+UUID.randomUUID()+";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("create table posts(id varchar(36) primary key,author_id varchar(36),content clob,status varchar(10),created_at timestamp,version bigint default 0)");
        jdbc.execute("create table post_reactions(id varchar(36))");jdbc.execute("create table saved_post(id varchar(36))");
        String xml=new String(getClass().getResourceAsStream("/config/liquibase/changelog/20261017000000_administration.xml").readAllBytes(),StandardCharsets.UTF_8);
        for(String statement:xml.substring(xml.indexOf("<![CDATA[")+9,xml.indexOf("]]>" )).split(";")) if(!statement.isBlank()) jdbc.execute(statement);
        String actor=UUID.randomUUID().toString();UUID id=UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("verified").header("alg","RS256").subject(actor).build(),List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        jdbc.update("insert into posts(id,author_id,content,status,created_at) values(?,?,?,'ACTIVE',CURRENT_TIMESTAMP)",id.toString(),UUID.randomUUID().toString(),"PRIVATE CONTENT");
        Outbox outbox=mock(Outbox.class,invocation->{throw new IllegalStateException("durable audit unavailable");});
        var directory=mock(AccountDirectory.class);when(directory.excluded()).thenReturn(List.of());
        var resource=new AdminContentResource(jdbc,new AdminJournal(jdbc,outbox),outbox,directory,mock(org.springframework.context.ApplicationEventPublisher.class));
        var listing=resource.list(0,20,"","","",null);
        assertThat(listing.toString()).doesNotContain("PRIVATE CONTENT");
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        assertThatThrownBy(()->transaction.execute(ignored -> resource.detail(id))).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from admin_audit",Long.class)).isZero();
        assertThatThrownBy(()->transaction.execute(ignored -> resource.visibility(id,"hide","command",new AdminJournal.Change("reason",0L)))).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select moderation_visibility from posts",String.class)).isEqualTo("VISIBLE");
        assertThat(jdbc.queryForObject("select version from posts",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select content from posts",String.class)).isEqualTo("PRIVATE CONTENT");
    }
}
