package com.campaignorganizer.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * HTTP-level coverage for the self-service recovery-code and TOTP-re-enrollment endpoints
 * (ADR-0111 follow-up) — the MFA-factor gate on all four, the actual regenerate/re-enrollment
 * round trips, and that the old PASSWORD-only enrollment route can't be used to re-enroll an
 * already-TOTP account (the exact class of bug PR #87's security review found for WebAuthn).
 */
class SelfServiceMfaRecoveryControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";
    private static final CodeGenerator CODE_GENERATOR = new DefaultCodeGenerator();
    private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

    @Autowired
    private AuthenticateAccountPort authenticateAccountPort;
    @Autowired
    private JwtService jwtService;

    @Test
    void allFourEndpointsRequireTheMfaFactorNotJustPassword() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        String passwordOnlyToken = passwordOnlyToken(enrolled.email());

        mockMvc.perform(get("/api/accounts/me/recovery-codes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/accounts/me/recovery-codes/regenerate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/accounts/me/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/accounts/me/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"111111\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recoveryCodeStatusReturnsTenRightAfterEnrollment() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        String fullToken = fullTotpToken(enrolled);

        mockMvc.perform(get("/api/accounts/me/recovery-codes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(10));
    }

    @Test
    void regeneratingRecoveryCodesInvalidatesOldCodesAndIssuesNewOnes() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        String fullToken = fullTotpToken(enrolled);

        String body = mockMvc.perform(post("/api/accounts/me/recovery-codes/regenerate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> newCodes = JsonPath.read(body, "$");
        assertThat(newCodes).hasSize(10);
        assertThat(newCodes).doesNotContainAnyElementsOf(enrolled.recoveryCodes());

        // The old codes no longer work...
        String oldCodeChallengeToken = login(enrolled.email()).token();
        mockMvc.perform(post("/api/auth/mfa/verify-recovery-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldCodeChallengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryCode\":\"" + enrolled.recoveryCodes().get(0) + "\"}"))
                .andExpect(status().isUnauthorized());

        // ...but a new one does.
        String newCodeChallengeToken = login(enrolled.email()).token();
        mockMvc.perform(post("/api/auth/mfa/verify-recovery-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newCodeChallengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryCode\":\"" + newCodes.get(0) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_SETUP_REQUIRED"));
    }

    @Test
    void totpReEnrollmentReplacesTheActiveSecret() throws Exception {
        Enrolled enrolled = registerAndEnrollTotp();
        String fullToken = fullTotpToken(enrolled);

        String startBody = mockMvc.perform(post("/api/accounts/me/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newSecret = JsonPath.read(startBody, "$.secret");

        mockMvc.perform(post("/api/accounts/me/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(newSecret) + "\"}"))
                .andExpect(status().isNoContent());

        // The old secret's codes no longer challenge successfully...
        String oldSecretChallengeToken = login(enrolled.email()).token();
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldSecretChallengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(enrolled.secret()) + "\"}"))
                .andExpect(status().isUnauthorized());

        // ...but the new secret's codes do.
        String newSecretChallengeToken = login(enrolled.email()).token();
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newSecretChallengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(newSecret) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void totpReEnrollmentConfirmWithWrongCodeReturns400NotAnAuthFailure() throws Exception {
        // Deliberately 400 (a bad input), not 401 (an auth failure): the caller here already
        // holds a full, valid session — the frontend's shared request() helper clears the
        // stored session token on any 401, which would otherwise silently log the user out of
        // an already-valid session just for mistyping a 6-digit code.
        Enrolled enrolled = registerAndEnrollTotp();
        String fullToken = fullTotpToken(enrolled);

        mockMvc.perform(post("/api/accounts/me/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts/me/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totpReEnrollmentFailsWhenNoMfaMethodIsActiveYet() throws Exception {
        String email = register();
        AccountView account = authenticateAccountPort.authenticate(email, PASSWORD).orElseThrow();
        String fullToken = jwtService.issue(account.id(), account.role(), account.tokenVersion()).token();

        mockMvc.perform(post("/api/accounts/me/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fullToken))
                .andExpect(status().isBadRequest());
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
        return new LoginResult(JsonPath.read(body, "$.token"));
    }

    private Enrolled registerAndEnrollTotp() throws Exception {
        String email = register();
        String setupToken = login(email).token();
        String startBody = mockMvc.perform(post("/api/auth/mfa/setup/totp/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(startBody, "$.secret");
        String confirmBody = mockMvc.perform(post("/api/auth/mfa/setup/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(secret) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> recoveryCodes = JsonPath.read(confirmBody, "$.recoveryCodes");
        return new Enrolled(email, secret, recoveryCodes);
    }

    /** A fully-authenticated (PASSWORD+MFA) token, minted the same way a real TOTP challenge would. */
    private String fullTotpToken(Enrolled enrolled) throws Exception {
        String challengeToken = login(enrolled.email()).token();
        String body = mockMvc.perform(post("/api/auth/mfa/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + challengeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(enrolled.secret()) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String passwordOnlyToken(String email) throws Exception {
        AccountView account = authenticateAccountPort.authenticate(email, PASSWORD).orElseThrow();
        return jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                Set.of(JwtService.PASSWORD_FACTOR)).token();
    }

    private static String currentCode(String secret) throws Exception {
        long counter = TIME_PROVIDER.getTime() / 30;
        return CODE_GENERATOR.generate(secret, counter);
    }

    private record LoginResult(String token) {
    }

    private record Enrolled(String email, String secret, List<String> recoveryCodes) {
    }
}
