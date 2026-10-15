package com.minh.fakebook.post.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.minh.fakebook.post.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class LoginBoundaryIT {
    @Autowired MockMvc mvc;
    @ParameterizedTest
    @ValueSource(strings = {"/api/posts", "/api/posts/count", "/api/posts/00000000-0000-4000-8000-000000000001", "/api/posts/profile/00000000-0000-4000-8000-000000000001", "/api/post-reactions/summaries"})
    void businessReadsRequireLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
