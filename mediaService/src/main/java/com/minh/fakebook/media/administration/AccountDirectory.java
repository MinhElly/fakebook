package com.minh.fakebook.media.administration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
@Component
public class AccountDirectory {
    private final InternalHttp http;
    private final String base;
    public AccountDirectory(InternalHttp http, @Value("${USER_SERVICE_URL:http://localhost:8082}") String base) { this.http=http; this.base=base; }
    public AccountState state(String subject) {
        return http.client().get().uri(base+"/api/internal/accounts/{subject}",subject)
            .headers(h -> h.setBearerAuth(http.token())).retrieve().body(AccountState.class);
    }
    public List<String> excluded() {
        var result=http.client().get().uri(base+"/api/internal/accounts/excluded")
            .headers(h -> h.setBearerAuth(http.token())).retrieve().body(new ParameterizedTypeReference<List<String>>() {});
        if (result==null) throw new IllegalStateException("Account directory unavailable");
        return result;
    }
}
