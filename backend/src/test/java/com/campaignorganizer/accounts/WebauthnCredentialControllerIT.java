package com.campaignorganizer.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.campaignorganizer.accounts.adapter.account.out.mfa.WebAuthnUserHandle;
import com.campaignorganizer.accounts.adapter.account.out.persistence.WebAuthnCredentialJpaRepository;
import com.campaignorganizer.accounts.adapter.account.out.persistence.WebAuthnCredentialRepositoryAdapter;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.security.JwtService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

/**
 * HTTP-level coverage for self-service passkey management (ADR-0111 follow-up). Credential rows
 * are seeded via the real {@link WebAuthnCredentialRepositoryAdapter#save} — the same path
 * Spring's own registration ceremony uses — rather than a real end-to-end WebAuthn ceremony
 * (covered by {@code WebAuthnCeremonyWiringIT}); this class is about the plain CRUD
 * (list/remove) layer built on top.
 */
class WebauthnCredentialControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "integration-test-password";

    @Autowired
    private WebAuthnCredentialJpaRepository credentialRepository;
    @Autowired
    private WebAuthnCredentialRepositoryAdapter credentialAdapter;
    @Autowired
    private AuthenticateAccountPort authenticateAccountPort;
    @Autowired
    private JwtService jwtService;

    @Test
    void listAndRemoveRequireTheMfaFactorNotJustPassword() throws Exception {
        String email = register();
        AccountView account = authenticateAccountPort.authenticate(email, PASSWORD).orElseThrow();
        String passwordOnlyToken = jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                java.util.Set.of(JwtService.PASSWORD_FACTOR)).token();

        mockMvc.perform(get("/api/accounts/me/webauthn-credentials")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/accounts/me/webauthn-credentials/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordOnlyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsOnlyTheCallersOwnCredentials() throws Exception {
        String auth = authHeader();
        UUID accountId = currentAccountId(auth);
        seedCredential(accountId, "Laptop");
        seedCredential(accountId, "Phone");
        // A different account's credential must never show up in this list.
        seedCredential(currentAccountId(authHeader()), "Someone else's key");

        mockMvc.perform(get("/api/accounts/me/webauthn-credentials").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("Laptop"))
                .andExpect(jsonPath("$[1].label").value("Phone"));
    }

    @Test
    void removingOneOfMultipleCredentialsSucceeds() throws Exception {
        String auth = authHeader();
        UUID accountId = currentAccountId(auth);
        UUID first = seedCredential(accountId, "Laptop");
        seedCredential(accountId, "Phone");

        mockMvc.perform(delete("/api/accounts/me/webauthn-credentials/{id}", first)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/me/webauthn-credentials").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Phone"));
    }

    @Test
    void cannotRemoveAnotherAccountsCredential() throws Exception {
        String auth = authHeader();
        String otherAuth = authHeader();
        UUID otherAccountId = currentAccountId(otherAuth);
        UUID otherCredential = seedCredential(otherAccountId, "Not yours");

        mockMvc.perform(delete("/api/accounts/me/webauthn-credentials/{id}", otherCredential)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/accounts/me/webauthn-credentials").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String register() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@test.local";
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isAccepted());
        return email;
    }

    /** Uses the {@code sub} claim rather than a new lookup — cheaper than parsing the JWT ourselves. */
    private UUID currentAccountId(String authHeader) throws Exception {
        String body = mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, authHeader))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(body, "$.id"));
    }

    /**
     * Seeds a credential the same way Spring's own registration ceremony would (via {@link
     * WebAuthnCredentialRepositoryAdapter#save}) — the account is freshly registered
     * ({@code mfaMethod == NONE}), so this always takes the first-enrollment branch of that
     * adapter's guard regardless of how many credentials the account already has, with no
     * MFA-factor SecurityContext setup needed here.
     */
    private UUID seedCredential(UUID accountId, String label) {
        byte[] credentialId = UUID.randomUUID().toString().getBytes();
        Instant now = Instant.now();
        CredentialRecord record = ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(new Bytes(credentialId))
                .userEntityUserId(WebAuthnUserHandle.toBytes(accountId))
                .publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3}))
                .signatureCount(0)
                .uvInitialized(true)
                .transports(Set.of())
                .backupEligible(false)
                .backupState(false)
                .attestationObject(new Bytes(new byte[] {4, 5, 6}))
                .attestationClientDataJSON(new Bytes(new byte[] {7, 8, 9}))
                .label(label)
                .created(now)
                .lastUsed(now)
                .build();
        credentialAdapter.save(record);
        return credentialRepository.findByCredentialId(credentialId).orElseThrow().getId();
    }
}
