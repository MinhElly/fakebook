package com.minh.fakebook.comment.administration;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;

class CommentModerationTest {
    @Test void restoreParentPreservesIndependentChildDecisionAndAdminArchive() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:branches"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("create table comments(id varchar(36) primary key,post_id varchar(36),parent_comment_id varchar(36),moderation_visibility varchar(10),community_archived boolean,effective_hidden boolean)");
        jdbc.update("insert into comments values('parent','post',null,'HIDDEN',false,false),('child','post','parent','HIDDEN',false,false),('grandchild','post','child','VISIBLE',false,false),('sibling','post','parent','VISIBLE',false,false)");
        CommentModeration.recompute(jdbc,"post");
        assertThat(jdbc.queryForObject("select count(*) from comments where effective_hidden=true",Long.class)).isEqualTo(4);
        jdbc.update("update comments set moderation_visibility='VISIBLE' where id='parent'");
        CommentModeration.recompute(jdbc,"post");
        assertThat(jdbc.queryForObject("select effective_hidden from comments where id='sibling'",Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select effective_hidden from comments where id='grandchild'",Boolean.class)).isTrue();
        jdbc.update("update comments set community_archived=true where id='parent'");
        CommentModeration.recompute(jdbc,"post");
        assertThat(jdbc.queryForObject("select count(*) from comments where effective_hidden=true",Long.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from comments",Long.class)).isEqualTo(4);
    }
}
