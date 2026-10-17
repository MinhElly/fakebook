package com.minh.fakebook.post.administration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
class AccountAccessFilterTest {
    AccountDirectory directory=mock(AccountDirectory.class);
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}
    private MockHttpServletResponse request(String role,String path,AccountState state) throws Exception {
        var jwt=Jwt.withTokenValue("verified").header("alg","RS256").subject("11111111-1111-1111-1111-111111111111")
            .issuedAt(Instant.parse("2026-10-17T00:00:00Z")).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,java.util.Arrays.stream(role.split(",")).map(SimpleGrantedAuthority::new).toList()));
        when(directory.state(jwt.getSubject())).thenReturn(state);
        var request=new MockHttpServletRequest("GET",path);request.setServletPath(path);
        var response=new MockHttpServletResponse();
        new AccountAccessFilter(directory).doFilter(request,response,(req,res)->res.getWriter().write("allowed"));
        return response;
    }
    private AccountState state(String kind,String status,Instant watermark) {
        return new AccountState("11111111-1111-1111-1111-111111111111",kind,status,watermark,false,0,"SYNCED");
    }
    @Test void adminCannotEnterCommunity() throws Exception {
        assertThat(request("ROLE_ADMIN","/api/posts",state("ADMIN","ACTIVE",null)).getStatus()).isEqualTo(403);
        assertThat(request("ROLE_ADMIN","/api/admin/posts",state("ADMIN","ACTIVE",null)).getContentAsString()).isEqualTo("allowed");
    }
    @Test void adminWithUserRoleStillCannotEnterCommunity() throws Exception {
        assertThat(request("ROLE_ADMIN,ROLE_USER","/api/posts",state("ADMIN","ACTIVE",null)).getStatus()).isEqualTo(403);
    }
    @Test void userAndInternalCannotAdminister() throws Exception {
        assertThat(request("ROLE_USER","/api/admin/posts",state("USER","ACTIVE",null)).getStatus()).isEqualTo(403);
        assertThat(request("ROLE_INTERNAL","/api/admin/posts",null).getStatus()).isEqualTo(403);
        assertThat(request("ROLE_INTERNAL","/api/internal/accounts/excluded",null).getContentAsString()).isEqualTo("allowed");
    }
    @Test void oldTokensStopImmediatelyAndDirectoryFailureClosesAccess() throws Exception {
        var suspended=request("ROLE_USER","/api/posts",state("USER","SUSPENDED",null));
        assertThat(suspended.getStatus()).isEqualTo(403);assertThat(suspended.getContentAsString()).contains("ACCOUNT_SUSPENDED");
        assertThat(request("ROLE_USER","/api/posts",state("USER","ACTIVE",Instant.parse("2026-10-17T00:00:01Z"))).getContentAsString()).contains("TOKEN_REVOKED");
        assertThat(request("ROLE_USER","/api/posts",null).getStatus()).isEqualTo(503);
    }
    @Test void staleRoleTokenCannotBypassDirectory() throws Exception {
        assertThat(request("ROLE_USER","/api/posts",state("ADMIN","ACTIVE",null)).getContentAsString()).contains("TOKEN_REVOKED");
    }
}
