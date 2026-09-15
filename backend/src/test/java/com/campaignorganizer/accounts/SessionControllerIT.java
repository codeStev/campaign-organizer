package com.campaignorganizer.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * HTTP-level coverage for self-service session/device tracking (ADR-0112) — a second,
 * finer-grained revocation layer alongside the account-wide {@code logout-all}/{@code
 * tokenVersion} mechanism {@code AccountControllerIT}-adjacent tests already cover.
 */
class SessionControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";
    private static final CodeGenerator CODE_GENERATOR = new DefaultCodeGenerator();
    private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

    @Autowired
    private AuthenticateAccountPort authenticateAccountPort;
    @Autowired
    private JwtService jwtService;

    @Test
    void listAndRevokeRequireTheMfaFactorNotJustPassword() throws Exception {
        String email = register();
        AccountView account = authenticateAccountPort.authenticate(email, PASSWORD).orElseThrow();
        String passwordOnlyToken = jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                Set.of(JwtService.PASSWORD_FACTOR)).token();

        mockMvc.perform(get("/api/accounts/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void aFreshLoginsOwnSessionIsListedAndFlaggedCurrent() throws Exception {
        String auth = authHeader();

        mockMvc.perform(get("/api/accounts/me/sessions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    void revokingADifferentSessionInvalidatesOnlyThatTokenNotTheCallersOwn() throws Exception {
        // Enrollment confirm itself mints a full token too, so this account already has one
        // "orphan" session neither tokenA nor tokenB holds — sessionBId is read via tokenB's own
        // list rather than inferred by elimination, so that orphan can't be picked by mistake.
        Enrolled enrolled = registerAndEnrollTotp();
        String tokenA = fullTotpToken(enrolled);
        String tokenB = fullTotpToken(enrolled);
        String sessionAId = ownCurrentSessionId(tokenA);
        String sessionBId = ownCurrentSessionId(tokenB);

        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", sessionBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // Token B's own session is gone — it 401s on the very next request...
        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());
        // ...but token A, whose session was never touched, keeps working.
        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/accounts/me/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(Matchers.not(Matchers.hasItem(sessionBId))))
                .andExpect(jsonPath("$[*].id").value(Matchers.hasItem(sessionAId)));
    }

    @Test
    void revokingOwnCurrentSessionMakesItsOwnNextRequestFail() throws Exception {
        String auth = authHeader();
        String listBody = mockMvc.perform(get("/api/accounts/me/sessions").header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString();
        String ownSessionId = JsonPath.read(listBody, "$[0].id");

        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", ownSessionId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotRevokeAnotherAccountsSession() throws Exception {
        String auth = authHeader();
        String otherAuth = authHeader();
        String otherListBody = mockMvc.perform(get("/api/accounts/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andReturn().getResponse().getContentAsString();
        String otherSessionId = JsonPath.read(otherListBody, "$[0].id");

        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", otherSessionId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        // The other account's session survives — still usable, still listed.
        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk());
    }

    @Test
    void revokingAnAlreadyRevokedSessionFails() throws Exception {
        // Two full tokens for one account, so the first revoke still leaves a usable session
        // (tokenA's) to authenticate the second, failing attempt with.
        Enrolled enrolled = registerAndEnrollTotp();
        String tokenA = fullTotpToken(enrolled);
        String tokenB = fullTotpToken(enrolled);
        String sessionBId = ownCurrentSessionId(tokenB);

        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", sessionBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/accounts/me/sessions/{id}", sessionBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    /** Reads a token's own session id from its {@code current: true} entry in its own listing. */
    private String ownCurrentSessionId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/accounts/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> currentIds = JsonPath.read(body, "$[?(@.current == true)].id");
        return currentIds.get(0);
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
        mockMvc.perform(post("/api/auth/mfa/setup/totp/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + setupToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentCode(secret) + "\"}"))
                .andExpect(status().isOk());
        return new Enrolled(email, secret);
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

    private static String currentCode(String secret) throws Exception {
        long counter = TIME_PROVIDER.getTime() / 30;
        return CODE_GENERATOR.generate(secret, counter);
    }

    private record LoginResult(String token) {
    }

    private record Enrolled(String email, String secret) {
    }
}
