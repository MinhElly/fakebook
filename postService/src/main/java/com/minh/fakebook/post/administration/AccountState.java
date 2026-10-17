package com.minh.fakebook.post.administration;

import java.time.Instant;
public record AccountState(String subject, String accountKind, String status, Instant tokensValidAfter,
    boolean communityArchived, long version, String syncState) {
    public String denial(Instant issuedAt, boolean adminClaim, boolean adminEndpoint, boolean accountEndpoint) {
        if (!"ACTIVE".equals(status)) return "ACCOUNT_SUSPENDED";
        if (tokensValidAfter != null && (issuedAt == null || !issuedAt.isAfter(tokensValidAfter))) return "TOKEN_REVOKED";
        boolean admin = "ADMIN".equals(accountKind);
        if (admin != adminClaim) return "TOKEN_REVOKED";
        if (adminEndpoint && !admin) return "ADMIN_REQUIRED";
        if (!adminEndpoint && !accountEndpoint && (admin || communityArchived)) return "COMMUNITY_USER_REQUIRED";
        return null;
    }
}
