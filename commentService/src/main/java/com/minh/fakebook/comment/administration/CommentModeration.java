package com.minh.fakebook.comment.administration;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
public final class CommentModeration {
    private CommentModeration() {}
    /** Preserve explicit child decisions; recompute the effective whole branch under a post lock. */
    public static void recompute(JdbcTemplate jdbc,String post) {
        var rows=jdbc.queryForList("select id,parent_comment_id,moderation_visibility,community_archived from comments where post_id=?",post);
        Map<String,Map<String,Object>> byId=new HashMap<>(); rows.forEach(r -> byId.put(r.get("id").toString(),r));
        for (var row:rows) {
            boolean hidden=false; var cursor=row; Set<String> visited=new HashSet<>();
            while (cursor!=null) {
                String id=cursor.get("id").toString();
                if (!visited.add(id)) { hidden=true; break; }
                if ("HIDDEN".equals(cursor.get("moderation_visibility")) || Boolean.TRUE.equals(cursor.get("community_archived"))) { hidden=true; break; }
                Object parent=cursor.get("parent_comment_id"); cursor=parent==null?null:byId.get(parent.toString());
            }
            jdbc.update("update comments set effective_hidden=? where id=?",hidden,row.get("id"));
        }
    }
}
