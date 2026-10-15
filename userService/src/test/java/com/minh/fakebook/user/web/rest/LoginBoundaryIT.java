package com.minh.fakebook.user.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.minh.fakebook.user.IntegrationTest;
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
    @ValueSource(strings = {"/api/user-profiles/public", "/api/user-profiles/00000000-0000-4000-8000-000000000001/details"})
    void businessReadsRequireLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
