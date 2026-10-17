package com.minh.fakebook.user.administration;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountResource {
    private final AccountDirectory directory;
    public AccountResource(AccountDirectory directory) { this.directory=directory; }
    @GetMapping("/api/internal/accounts/{subject}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL')")
    public AccountState state(@PathVariable String subject) { return directory.state(subject); }
    @GetMapping("/api/internal/accounts/excluded")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL')")
    public java.util.List<String> excluded() { return directory.excluded(); }
    @GetMapping("/api/account-state")
    public AccountState current(@AuthenticationPrincipal Jwt jwt) { return directory.state(jwt.getSubject()); }
}
