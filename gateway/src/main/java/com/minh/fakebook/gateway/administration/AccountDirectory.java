package com.minh.fakebook.gateway.administration;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.minh.fakebook.gateway.client.InternalServiceTokenProvider;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
@Component
public class AccountDirectory {
    private final WebClient http;
    private final InternalServiceTokenProvider tokens;
    public AccountDirectory(WebClient.Builder builder,InternalServiceTokenProvider tokens,@Value("${USER_SERVICE_URL:http://localhost:8082}") String base) {
        this.http=builder.clone().baseUrl(base).build();this.tokens=tokens;
    }
    public Mono<AccountState> state(String subject) {
        return Mono.defer(() -> tokens.getAccessToken().flatMap(token -> http.get().uri("/api/internal/accounts/{subject}",subject)
            .headers(h -> h.setBearerAuth(token)).retrieve().bodyToMono(AccountState.class)))
            .timeout(Duration.ofSeconds(5)).switchIfEmpty(Mono.error(new IllegalStateException("Account state missing")))
            .onErrorMap(error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"ACCOUNT_STATE_UNAVAILABLE"));
    }
    public Mono<Void> community(String subject,Instant issuedAt) {
        return state(subject).flatMap(state -> {
            String denial=state.denial(issuedAt,false,false,false);
            return denial==null?Mono.empty():Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,denial));
        });
    }
}
