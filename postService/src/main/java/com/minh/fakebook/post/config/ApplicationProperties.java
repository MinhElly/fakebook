package com.minh.fakebook.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Post Service.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();
    private final Cache cache = new Cache();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Cache getCache() {
        return cache;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    public static class Cache {

        private final ReactionCounts reactionCounts = new ReactionCounts();
        private final PostContent postContent = new PostContent();

        public ReactionCounts getReactionCounts() {
            return reactionCounts;
        }

        public PostContent getPostContent() {
            return postContent;
        }

        public static class ReactionCounts {

            private boolean enabled = true;
            private int ttlSeconds = 20;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getTtlSeconds() {
                return ttlSeconds;
            }

            public void setTtlSeconds(int ttlSeconds) {
                this.ttlSeconds = ttlSeconds;
            }
        }

        public static class PostContent {

            private boolean enabled = true;
            private int ttlSeconds = 300;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getTtlSeconds() {
                return ttlSeconds;
            }

            public void setTtlSeconds(int ttlSeconds) {
                this.ttlSeconds = ttlSeconds;
            }
        }
    }

    // jhipster-needle-application-properties-property-class
}
