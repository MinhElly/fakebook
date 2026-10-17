package com.minh.fakebook.user.administration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Refresh identity display data in batches; authorization always reads the account directory. */
@Component
public class AccountIdentitySync {
    private final AdminAccountService accounts;
    public AccountIdentitySync(AdminAccountService accounts) { this.accounts=accounts; }

    @Scheduled(fixedDelayString="${administration.identity-sync-interval:300000}")
    public void refresh() {
        try { accounts.refreshIdentityMetadata(); }
        catch (Exception unavailable) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Account identity metadata refresh unavailable; will retry.");
        }
    }
}
