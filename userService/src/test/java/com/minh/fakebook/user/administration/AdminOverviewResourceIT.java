package com.minh.fakebook.user.administration;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import com.minh.fakebook.user.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class AdminOverviewResourceIT {
    @Autowired MockMvc mvc;
    @MockitoBean AccountDirectory directory;
    static final String ADMIN="11111111-1111-1111-1111-111111111111";
    static final String USER="22222222-2222-2222-2222-222222222222";
    @BeforeEach void directory() {
        when(directory.state(ADMIN)).thenReturn(new AccountState(ADMIN,"ADMIN","ACTIVE",null,false,0,"SYNCED"));
        when(directory.state(USER)).thenReturn(new AccountState(USER,"USER","ACTIVE",null,false,0,"SYNCED"));
    }
    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token(String subject,String role) {
        return jwt().jwt(claims -> claims.subject(subject).issuedAt(Instant.now())).authorities(new SimpleGrantedAuthority(role));
    }
    @Test void adminReceivesDefaultSevenDaysAndThirtyDaysWithoutAuditIdentities() throws Exception {
        mvc.perform(get("/api/admin/overview").with(token(ADMIN,"ROLE_ADMIN"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.users").isNumber()).andExpect(jsonPath("$.admins").isNumber())
            .andExpect(jsonPath("$.pending").isNumber()).andExpect(jsonPath("$.directory_ready").isNumber())
            .andExpect(jsonPath("$.user_states.active").isNumber()).andExpect(jsonPath("$.sync_states.synced").isNumber())
            .andExpect(jsonPath("$.timezone").value("Asia/Ho_Chi_Minh")).andExpect(jsonPath("$.days").value(7))
            .andExpect(jsonPath("$.activity",hasSize(7))).andExpect(jsonPath("$.activity[0].actor").doesNotExist());
        mvc.perform(get("/api/admin/overview").param("days","30").with(token(ADMIN,"ROLE_ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.days").value(30)).andExpect(jsonPath("$.activity",hasSize(30)));
    }
    @Test void rejectsUnsupportedAndMalformedPeriods() throws Exception {
        for(String period:new String[]{"0","14","365","invalid"})
            mvc.perform(get("/api/admin/overview").param("days",period).with(token(ADMIN,"ROLE_ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void userAndInternalClientCannotReadOverview() throws Exception {
        mvc.perform(get("/api/admin/overview").with(token(USER,"ROLE_USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").with(token(USER,"ROLE_INTERNAL"))).andExpect(status().isForbidden());
    }
    @Test void anonymousCannotReadOverview() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
    }
}
