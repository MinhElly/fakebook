package com.minh.fakebook.comment.administration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;

class CommunityFilterTest {
    @Test void adminAncestorHidesOtherAuthorsRepliesBeforeArchival() {
        UUID admin=UUID.randomUUID(),user=UUID.randomUUID(),post=UUID.randomUUID();
        var directory=mock(AccountDirectory.class);when(directory.excluded()).thenReturn(List.of(admin.toString()));
        var beans=new DefaultListableBeanFactory();beans.registerSingleton("directory",directory);
        var configuration=new Configuration().addAnnotatedClass(Comment.class);
        configuration.setProperty("hibernate.connection.driver_class","org.h2.Driver");
        configuration.setProperty("hibernate.connection.url","jdbc:h2:mem:commentfilter"+UUID.randomUUID()+";MODE=MariaDB;DB_CLOSE_DELAY=-1");
        configuration.setProperty("hibernate.hbm2ddl.auto","create-drop");
        configuration.getProperties().put("hibernate.resource.beans.container",new SpringBeanContainer(beans));
        try(SessionFactory factory=configuration.buildSessionFactory()) {
            try(var session=factory.openSession()) {
                session.doWork(c -> {try(var s=c.createStatement()) {
                    s.execute("alter table comments add community_archived boolean default false not null");
                    s.execute("alter table comments add moderation_visibility varchar(10) default 'VISIBLE' not null");
                    s.execute("alter table comments add effective_hidden boolean default false not null");
                }});
                var tx=session.beginTransaction();
                var parent=new Comment().postId(post).authorId(admin).content("legacy").status(CommentStatus.ACTIVE);
                var child=new Comment().postId(post).authorId(user).content("reply").status(CommentStatus.ACTIVE);child.setParentComment(parent);
                var grandchild=new Comment().postId(post).authorId(user).content("nested").status(CommentStatus.ACTIVE);grandchild.setParentComment(child);
                session.persist(parent);session.persist(child);session.persist(grandchild);tx.commit();
            }
            try(var session=factory.openSession()) {
                assertThat(session.createSelectionQuery("select count(c) from Comment c",Long.class).getSingleResult()).isZero();
                assertThat(session.createSelectionQuery("from Comment c",Comment.class).setMaxResults(1).getResultList()).isEmpty();
            }
            when(directory.excluded()).thenReturn(List.of());
            try(var session=factory.openSession()) {
                // Simulate a reply racing moderation: effective flags have not been recomputed yet.
                session.doWork(c -> {try(var s=c.prepareStatement("update comments set moderation_visibility='HIDDEN' where author_id=?")) {s.setString(1,admin.toString());s.executeUpdate();}});
                assertThat(session.createSelectionQuery("select count(c) from Comment c",Long.class).getSingleResult()).isZero();
            }
        }
    }
}
