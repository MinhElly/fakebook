package com.minh.fakebook.post.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.*;

class CommunityFilterTest {
    SessionFactory factory;
    AccountDirectory directory;
    UUID admin=UUID.randomUUID();
    @BeforeEach void setup() {
        directory=mock(AccountDirectory.class);
        when(directory.excluded()).thenReturn(List.of(admin.toString()));
        var beans=new DefaultListableBeanFactory();
        beans.registerSingleton("directory",directory);
        beans.registerSingleton("excluded",new ExcludedSubjects(beans.getBeanProvider(AccountDirectory.class)));
        beans.registerSingleton("administrationView",new AdministrationView());
        var configuration=new Configuration().addAnnotatedClass(Post.class)
            .addAnnotatedClass(com.minh.fakebook.post.domain.PostMedia.class)
            .addAnnotatedClass(com.minh.fakebook.post.domain.PostReaction.class)
            .addAnnotatedClass(com.minh.fakebook.post.domain.SavedPost.class);
        configuration.setProperty("hibernate.connection.driver_class","org.h2.Driver");
        configuration.setProperty("hibernate.connection.url","jdbc:h2:mem:filter"+UUID.randomUUID()+";MODE=MariaDB;DB_CLOSE_DELAY=-1");
        configuration.setProperty("hibernate.hbm2ddl.auto","create-drop");
        configuration.getProperties().put("hibernate.resource.beans.container",new SpringBeanContainer(beans));
        factory=configuration.buildSessionFactory();
        try(var session=factory.openSession()) {
            session.doWork(connection -> {
                try(var statement=connection.createStatement()) {
                    statement.execute("alter table posts add community_archived boolean default false not null");
                    statement.execute("alter table posts add moderation_visibility varchar(10) default 'VISIBLE' not null");
                    statement.execute("alter table post_reactions add community_archived boolean default false not null");
                    statement.execute("alter table saved_post add community_archived boolean default false not null");
                }
            });
        }
    }
    @AfterEach void close() { if(factory!=null) factory.close();SecurityContextHolder.clearContext(); }
    @Test void filtersBeforeCountPaginationAndDirectIdAndRespondsToFreshDirectory() {
        UUID hidden;
        try(var session=factory.openSession()) {
            var tx=session.beginTransaction();
            var own=new Post().authorId(UUID.randomUUID()).content("public").visibility(PostVisibility.PUBLIC).status(PostStatus.ACTIVE).createdAt(Instant.now());
            var adminPost=new Post().authorId(admin).content("admin legacy").visibility(PostVisibility.PUBLIC).status(PostStatus.ACTIVE).createdAt(Instant.now());
            session.persist(own);session.persist(adminPost);session.flush();hidden=adminPost.getId();tx.commit();
        }
        try(var session=factory.openSession()) {
            var tx=session.beginTransaction();
            // Insert references through SQL, as a delayed projection or historical row would.
            session.doWork(c -> {try(var s=c.prepareStatement("insert into post_media(id,media_id,post_id,display_order,created_at) values(?,?,?,0,CURRENT_TIMESTAMP)")) {
                s.setString(1,UUID.randomUUID().toString());s.setString(2,UUID.randomUUID().toString());s.setString(3,hidden.toString());s.executeUpdate();
            }});tx.commit();
        }
        try(var session=factory.openSession()) {
            assertThat(session.createSelectionQuery("select count(p) from Post p",Long.class).getSingleResult()).isEqualTo(1);
            assertThat(session.createSelectionQuery("from Post",Post.class).setMaxResults(1).getResultList()).hasSize(1);
            assertThat(session.find(Post.class,hidden)).isNull();
            assertThat(session.createSelectionQuery("select count(m) from PostMedia m",Long.class).getSingleResult()).isZero();
        }
        when(directory.excluded()).thenReturn(List.of());
        try(var session=factory.openSession()) {
            assertThat(session.createSelectionQuery("select count(p) from Post p",Long.class).getSingleResult()).isEqualTo(2);
            session.doWork(c -> {try(var s=c.prepareStatement("update posts set community_archived=true where id=?")) {s.setString(1,hidden.toString());s.executeUpdate();}});
        }
        try(var session=factory.openSession()) {
            assertThat(session.find(Post.class,hidden)).isNull();
            assertThat(session.createSelectionQuery("select count(p) from Post p",Long.class).getSingleResult()).isEqualTo(1);
        }
    }
}
