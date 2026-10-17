package com.minh.fakebook.media.administration;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AdminMediaService {
    private final JdbcTemplate jdbc;
    private final AdminJournal journal;
    public AdminMediaService(JdbcTemplate jdbc,AdminJournal journal) {this.jdbc=jdbc;this.journal=journal;}
    /** Returns only to the server-side proxy, after the local audit and outbox have committed. */
    @Transactional
    public String auditedLocation(UUID id) {
        var rows=jdbc.queryForList("select url,status from media where id=?",id.toString());
        if(rows.isEmpty() || !"ACTIVE".equals(rows.getFirst().get("status"))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        journal.append(id.toString(),"VIEW_MEDIA","Xem tệp phục vụ kiểm duyệt","METADATA","FILE",null,null,null);
        return rows.getFirst().get("url").toString();
    }
}
