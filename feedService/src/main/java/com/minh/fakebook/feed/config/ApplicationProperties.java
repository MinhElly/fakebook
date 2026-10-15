package com.minh.fakebook.feed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Feed Service.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();
    private final Feed feed = new Feed();

    public Feed getFeed() { return feed; }

    public static class Feed {
        private String cursorSecret = "";
        public String getCursorSecret() { return cursorSecret; }
        public void setCursorSecret(String value) { cursorSecret = value; }
    }


    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
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

    // jhipster-needle-application-properties-property-class
}
