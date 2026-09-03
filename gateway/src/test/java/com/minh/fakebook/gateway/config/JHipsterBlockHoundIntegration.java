package com.minh.fakebook.gateway.config;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

public class JHipsterBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("java.util.UUID", "randomUUID");
        builder.allowBlockingCallsInside("org.springframework.validation.beanvalidation.SpringValidatorAdapter", "validate");
        builder.allowBlockingCallsInside("com.minh.fakebook.gateway.service.MailService", "sendEmailFromTemplate");
        builder.allowBlockingCallsInside("com.minh.fakebook.gateway.security.DomainUserDetailsService", "createSpringSecurityUser");
        builder.allowBlockingCallsInside("org.springframework.web.reactive.result.method.InvocableHandlerMethod", "invoke");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "build");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "getWebhooksClasses");
        builder.allowBlockingCallsInside("org.springdoc.core.service.AbstractRequestService", "build");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.message.client.HandshakeResponse", "writeConnectAttributes");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.MariadbFrameDecoder", "decode");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "executeWhenTransaction");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "executeWhenNotInTransaction");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "sendCommand");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "sendCommandWithoutResult");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "setAutoCommit");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "lambda$receive$14");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "lambda$sendCommand$16");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.client.SimpleClient", "lambda$sendCommand$19");
        builder.allowBlockingCallsInside("org.mariadb.r2dbc.message.flow.AuthenticationFlow", "pluginRequireSecure");
        // jhipster-needle-blockhound-integration - JHipster will add additional gradle plugins here
    }
}
