package com.campaignorganizer.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** End-to-end HTTP coverage of the TOTP setup/challenge/recovery flow (ADR-0111). */
class MfaControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";
    private static final CodeGenerator CODE_GENERATOR = new DefaultCodeGenerator();
    private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

    @Test
    void totpSetupAndConfirmIssuesFullyAuthenticatedTokenAndRecoveryCodes() throws Exception {
        String email = register();
        String setupToken = login(email).token();

        String secret = startTotpSetup(setupToken).secret();
        String confirmBody = mockMvc.perform(post("/api/auth/mfa/setup/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(secret) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String fullToken = JsonPath.read(confirmBody, "$.token");
        List<String> recoveryCodes = JsonPath.read(confirmBody, "$.recoveryCodes");
        org.assertj.core.api.Assertions.assertThat(recoveryCodes).hasSize(10);

        mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reachable after MFA\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void secondLoginRequiresTotpChallengeAndVerifyIssuesFullToken() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();

        LoginResult challenge = login(enrolled.email());
        org.assertj.core.api.Assertions.assertThat(challenge.status()).isEqualTo("MFA_CHALLENGE_REQUIRED");
        org.assertj.core.api.Assertions.assertThat(challenge.method()).isEqualTo("TOTP");

        String body = mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + challenge.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(enrolled.secret()) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fullToken = JsonPath.read(body, "$.token");

        mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reachable after challenge\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void wrongTotpChallengeCodeIsRejected() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        LoginResult challenge = login(enrolled.email());

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + challenge.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recoveryCodeConsumptionForcesReEnrollmentAndCannotBeReused() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        LoginResult challenge = login(enrolled.email());
        String recoveryCode = enrolled.recoveryCodes().get(0);

        mockMvc.perform(post("/api/auth/mfa/verify-recovery-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + challenge.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryCode\":\"" + recoveryCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_SETUP_REQUIRED"));

        // The account is back to MFA_SETUP_REQUIRED on next login too.
        LoginResult afterRecovery = login(enrolled.email());
        org.assertj.core.api.Assertions.assertThat(afterRecovery.status()).isEqualTo("MFA_SETUP_REQUIRED");

        // The same code can't be spent twice.
        LoginResult secondChallenge = login(enrolled.email());
        mockMvc.perform(post("/api/auth/mfa/verify-recovery-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondChallenge.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryCode\":\"" + recoveryCode + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recoverPasswordResetsPasswordWithoutTouchingMfaMethod() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        String recoveryCode = enrolled.recoveryCodes().get(0);
        String newPassword = "brand-new-password-123";

        mockMvc.perform(post("/api/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + enrolled.email() + "\",\"recoveryCode\":\"" + recoveryCode
                                + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent());

        // Old password no longer works.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + enrolled.email() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        // New password works, and MFA is still required as TOTP (unaffected by the reset).
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + enrolled.email() + "\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_CHALLENGE_REQUIRED"))
                .andExpect(jsonPath("$.method").value("TOTP"))
                .andReturn().getResponse().getContentAsString();
        String challengeToken = JsonPath.read(body, "$.token");

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + challengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(enrolled.secret()) + "\"}"))
                .andExpect(status().isOk());
    }

    private Enrolled registerAndEnrollTotp() throws Exception {
        String email = register();
        String setupToken = login(email).token();
        TotpStart start = startTotpSetup(setupToken);
        String confirmBody = mockMvc.perform(post("/api/auth/mfa/setup/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(start.secret()) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> recoveryCodes = JsonPath.read(confirmBody, "$.recoveryCodes");
        return new Enrolled(email, start.secret(), recoveryCodes);
    }

    private String register() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@test.local";
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isAccepted());
        return email;
    }

    private LoginResult login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String status = JsonPath.read(body, "$.status");
        String token = JsonPath.read(body, "$.token");
        String method = JsonPath.read(body, "$.method");
        return new LoginResult(status, token, method);
    }

    private TotpStart startTotpSetup(String setupToken) throws Exception {
        String body = mockMvc.perform(post("/api/auth/mfa/setup/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new TotpStart(JsonPath.read(body, "$.secret"));
    }

    private static String currentCode(String secret) throws Exception {
        long counter = TIME_PROVIDER.getTime() / 30;
        return CODE_GENERATOR.generate(secret, counter);
    }

    private record LoginResult(String status, String token, String method) {
    }

    private record TotpStart(String secret) {
    }

    private record Enrolled(String email, String secret, List<String> recoveryCodes) {
    }
}
