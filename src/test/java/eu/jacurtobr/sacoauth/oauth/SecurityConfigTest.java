package eu.jacurtobr.sacoauth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void permitAllForOauthEndpoints() throws Exception {
        mockMvc.perform(get("/oauth/someEndpoint").with(csrf()))
                .andExpect(status().isFound());
    }

    @Test
    void authenticatedForOtherEndpoints() throws Exception {
        mockMvc.perform(get("/someOtherEndpoint"))
                .andExpect(status().isFound());
    }

    @Test
    void loginPageAccessible() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void successfulLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("password"))
                .andExpect(authenticated());
    }

    @Test
    void unsuccessfulLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("wrongPassword"))
                .andExpect(unauthenticated());
    }
}