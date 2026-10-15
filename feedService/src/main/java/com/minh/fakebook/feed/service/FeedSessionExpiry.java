package com.minh.fakebook.feed.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedSessionExpiry {
    private final JdbcTemplate jdbc;

    public FeedSessionExpiry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void removeExpiredSessions() {
        String cutoff = Instant.now().minus(Duration.ofMinutes(30)).toString();
        jdbc.update("DELETE p FROM home_feed_page p JOIN home_feed_session s ON s.id = p.session_id WHERE s.created_at < ?", cutoff);
        jdbc.update("DELETE p FROM home_feed_seen p JOIN home_feed_session s ON s.id = p.session_id WHERE s.created_at < ?", cutoff);
        jdbc.update("DELETE FROM home_feed_session WHERE created_at < ?", cutoff);
    }
}
