package com.campaignorganizer.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";

    /**
     * A fresh account has no MFA method yet (ADR-0111) — login succeeds at the password
     * check, but the response flags setup as required rather than handing back a directly
     * usable token. {@code com.campaignorganizer.accounts.MfaControllerIT} covers the rest
     * of the flow (setup, challenge, recovery) end-to-end.
     */
    @Test
    void loginWithFreshAccountRequiresMfaSetup() throws Exception {
        String email = register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_SETUP_REQUIRED"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    /** A password-only token (MFA not yet completed) can't reach ordinary API resources. */
    @Test
    void passwordOnlyTokenCannotAccessProtectedResources() throws Exception {
        String email = register();
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mockMvc.perform(post("/api/worlds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Should be rejected\"}"))
                .andExpect(status().isForbidden());
    }

    /** The same password-only token IS accepted by the MFA endpoints themselves. */
    @Test
    void passwordOnlyTokenCanReachMfaSetupEndpoint() throws Exception {
        String email = register();
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mockMvc.perform(post("/api/auth/mfa/setup/totp/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.qrCodeDataUri").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        String email = register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    /** Anti-enumeration (ADR-0110): an unknown email fails identically to a wrong password. */
    @Test
    void loginFailsWithUnknownEmailTheSameWayAsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-" + UUID.randomUUID() + "@test.local\","
                                + "\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        String email = register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsBlankEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Registers a fresh account for this test and returns its email. */
    private String register() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@test.local";
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isAccepted());
        return email;
    }
}
