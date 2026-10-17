package com.minh.fakebook.user.administration;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminAuditConsumer {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public AdminAuditConsumer(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    @KafkaListener(topics="admin-audit-events",groupId="userservice-admin-audit")
    @Transactional
    public void consume(String payload) {
        var e=mapper.readValue(payload,AdminAuditEvent.class);
        if(jdbc.queryForObject("select count(*) from admin_audit where event_id=?",Long.class,e.eventId())>0) return;
        try {
            jdbc.update("insert into admin_audit(event_id,actor,target,action,reason,occurred_at,before_state,after_state,request_id) values(?,?,?,?,?,?,?,?,?)",
                e.eventId(),e.actor(),e.target(),e.action(),e.reason(),Timestamp.from(e.occurredAt()),e.beforeState(),e.afterState(),e.requestId());
        } catch(org.springframework.dao.DuplicateKeyException replay) { /* Duplicate event delivery is harmless. */ }
    }
}
