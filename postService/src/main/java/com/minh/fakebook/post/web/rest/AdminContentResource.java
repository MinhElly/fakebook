package com.minh.fakebook.post.web.rest;
import com.minh.fakebook.post.administration.*;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import io.namastack.outbox.Outbox;
@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasAuthority('ROLE_ADMIN') and !hasAuthority('ROLE_INTERNAL')")
@Transactional
public class AdminContentResource {
    private final JdbcTemplate jdbc;
    private final AdminJournal journal;
    private final Outbox outbox;
    private final AccountDirectory directory;
    private final org.springframework.context.ApplicationEventPublisher events;
    public AdminContentResource(JdbcTemplate jdbc,AdminJournal journal,Outbox outbox,AccountDirectory directory,org.springframework.context.ApplicationEventPublisher events) {
        this.jdbc=jdbc; this.journal=journal; this.outbox=outbox; this.directory=directory;this.events=events;
    }
    @GetMapping
    @Transactional(readOnly=true)
    public Map<String,Object> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String visibility,
        @RequestParam(defaultValue="") String status,@RequestParam(required=false) Boolean archived) {
        page=Math.max(0,page); size=Math.clamp(size,1,100);
        String where=" where (lower(author_id) like ? or lower(id) like ?)";
        List<Object> args=new ArrayList<>(); args.add("%"+q.toLowerCase(Locale.ROOT)+"%");args.add(args.getFirst());
        if (!visibility.isBlank()) { where+=" and moderation_visibility=?";args.add(visibility); }
        if (!status.isBlank()) { where+=" and status=?";args.add(status); }
        if (archived!=null) { where+=" and community_archived=?";args.add(archived); }
        long total=jdbc.queryForObject("select count(*) from posts"+where,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        var items=jdbc.queryForList("select id,author_id,status,created_at,moderation_visibility,community_archived,version from posts"+where+" order by created_at desc,id desc limit ? offset ?",args.toArray());
        return Map.of("items",items,"total",total,"page",page,"size",size);
    }
    @GetMapping("/{id}")
    public Map<String,Object> detail(@PathVariable UUID id) {
        var rows=jdbc.queryForList("select * from posts where id=?",id.toString());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        journal.append(id.toString(),"VIEW_POST","Xem chi tiết phục vụ kiểm duyệt","METADATA","DETAIL",null,null,null);
        rows.getFirst().put("media_ids",jdbc.queryForList("select media_id from post_media where post_id=? order by display_order",String.class,id.toString()));
        return rows.getFirst();
    }
    @PostMapping("/{id}/{action:hide|restore}")
    public Map<String,Object> visibility(@PathVariable UUID id,@PathVariable String action,
        @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AdminJournal.Change change) {
        String operation=action.equals("hide")?"HIDE_POST":"RESTORE_POST";
        var replay=journal.replay(key,id.toString(),operation,change); if (replay!=null) return replay;

        var rows=jdbc.queryForList("select * from posts where id=? for update",id.toString());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var row=rows.getFirst(); long version=((Number)row.get("version")).longValue();
        if (version!=change.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"VERSION_CONFLICT");
        if (action.equals("restore") && (!"ACTIVE".equals(row.get("status")) || Boolean.TRUE.equals(row.get("community_archived")) || directory.excluded().contains(row.get("author_id").toString())))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"CONTENT_NOT_RESTORABLE");
        String next=action.equals("hide")?"HIDDEN":"VISIBLE";
        jdbc.update("update posts set moderation_visibility=?,version=version+1 where id=?",next,id.toString());

        events.publishEvent(new com.minh.fakebook.post.service.event.PostContentChanged(id));
        journal.append(id.toString(),operation,change.reason(),row.get("moderation_visibility").toString(),next,key,change,version+1);
        outbox.schedule(new ContentModerated("POST",id.toString(),id.toString(),row.get("author_id").toString(),next,version+1),"moderation-"+id,
            Map.of("destination","content-moderation-events"));
        return Map.of("version",version+1,"moderation_visibility",next,"replayed",false);
    }
    @io.namastack.outbox.annotation.OutboxEvent
    public record ContentModerated(String kind,String id,String postId,String authorId,String visibility,long version) {}
}
