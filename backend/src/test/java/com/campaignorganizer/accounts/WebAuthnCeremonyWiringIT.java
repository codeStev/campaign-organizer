package com.campaignorganizer.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/**
 * HTTP-level wiring coverage for the WebAuthn endpoints Spring Security itself serves
 * (SecurityConfig's {@code .webAuthn()} DSL, ADR-0111 follow-up): the scoped CSRF enforcement,
 * each endpoint's actual authentication requirement (verified against Spring Security 7.1.1's own
 * filter source — {@code PublicKeyCredentialCreationOptionsFilter} and
 * {@code PublicKeyCredentialRequestOptionsFilter} are both wired {@code addFilterBefore(...,
 * AuthorizationFilter.class)}, so they self-terminate the chain before SecurityConfig's own
 * {@code .requestMatchers("/webauthn/**", ...).authenticated()} rule ever runs; only
 * {@code WebAuthnRegistrationFilter} — the one that actually persists a credential — is wired
 * {@code addFilterAfter(AuthorizationFilter.class)} and so is the one that rule really gates), and
 * this app's own confirm endpoint's guard against activating WebAuthn without a stored credential.
 *
 * <p>A full cryptographically-simulated ceremony (create + register + authenticate with a fake
 * authenticator) isn't exercised here — that needs either a second test-only dependency whose
 * own JSON model doesn't necessarily match the wire format Spring's filters actually parse, or
 * hand-rolling COSE/CBOR signing, and would still only cover the backend, not the real ceremony
 * code in {@code client.ts}. That path is verified manually against a real browser/authenticator
 * instead (see the WebAuthn PR description).
 */
class WebAuthnCeremonyWiringIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";

    @Test
    void registrationOptionsRequireAnAuthenticatedPrincipalEvenWithAValidCsrfToken() throws Exception {
        // PublicKeyCredentialCreationOptionsFilter carries its own internal "any authenticated
        // principal" AuthorizationManager (not specific to the PASSWORD factor) — on denial it
        // writes 400 directly rather than throwing, so SecurityConfig's own accessDeniedHandler
        // never runs for this particular case.
        MvcResult loginResult = doLogin(register());
        String csrf = csrfTokenFrom(loginResult.getResponse());

        mockMvc.perform(post("/webauthn/register/options")
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie("XSRF-TOKEN", csrf)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticationOptionsRejectAnAnonymousCallerDespiteSpringNotRequiringOneItself() throws Exception {
        // PublicKeyCredentialRequestOptionsFilter itself never checks authentication (by the
        // spec's own "usernameless" resident-key login model, the server can't know who's
        // signing in before a credential is picked) — but this app's login flow always presents
        // a PASSWORD-factor pending token first anyway (same as TOTP's challenge step), so
        // WebAuthnRequestOptionsRepositoryAdapter enforces that itself rather than silently
        // crashing on an anonymous principal (it originally did: ClassCastException out of
        // CurrentUserPort, since that port's contract assumes an already-authenticated caller).
        MvcResult loginResult = doLogin(register());
        String csrf = csrfTokenFrom(loginResult.getResponse());

        mockMvc.perform(post("/webauthn/authenticate/options")
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie("XSRF-TOKEN", csrf)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void optionsEndpointsRejectRequestsWithoutAMatchingCsrfToken() throws Exception {
        String pendingToken = login(register());

        mockMvc.perform(post("/webauthn/register/options")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/webauthn/authenticate/options")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void optionsEndpointsSucceedWithAPendingTokenAndAMatchingCsrfToken() throws Exception {
        String email = register();
        MvcResult loginResult = doLogin(email);
        String pendingToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
        String csrf = csrfTokenFrom(loginResult.getResponse());

        mockMvc.perform(post("/webauthn/register/options")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken)
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie("XSRF-TOKEN", csrf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(email));

        mockMvc.perform(post("/webauthn/authenticate/options")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken)
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie("XSRF-TOKEN", csrf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").isNotEmpty());
    }

    @Test
    void registerCredentialEndpointRequiresAuthenticationViaTheOuterAuthorizationRule() throws Exception {
        // Unlike the options endpoints above, WebAuthnRegistrationFilter is wired
        // addFilterAfter(AuthorizationFilter.class) — SecurityConfig's own
        // .requestMatchers("/webauthn/**", ...).authenticated() rule really does gate this one.
        MvcResult loginResult = doLogin(register());
        String csrf = csrfTokenFrom(loginResult.getResponse());

        mockMvc.perform(post("/webauthn/register")
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie("XSRF-TOKEN", csrf))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmWebauthnSetupFailsWithoutARegisteredCredential() throws Exception {
        String pendingToken = login(register());

        mockMvc.perform(post("/api/auth/mfa/setup/webauthn/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken))
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

    private String login(String email) throws Exception {
        String body = doLogin(email).getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private MvcResult doLogin(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String csrfTokenFrom(MockHttpServletResponse response) {
        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).as("XSRF-TOKEN cookie should be set on every response under .spa() CSRF mode")
                .isNotNull();
        return cookie.getValue();
    }
}
