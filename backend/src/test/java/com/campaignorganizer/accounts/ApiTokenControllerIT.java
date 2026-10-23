package com.campaignorganizer.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** HTTP-level coverage for account API tokens (ADR-0119). */
class ApiTokenControllerIT extends AbstractIntegrationTest {

    private static final String BODY = "{\"name\":\"recorder\",\"scopes\":[\"glossary:read\",\"campaigns:read\"]}";

    @Test
    void managementRequiresAFullMfaSession() throws Exception {
        mockMvc.perform(get("/api/accounts/me/api-tokens")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/me/api-tokens").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createdTokenShowsItsSecretOnceAndIsListedWithoutIt() throws Exception {
        String auth = authHeader();

        String created = mockMvc.perform(post("/api/accounts/me/api-tokens")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(Matchers.startsWith("cot_")))
                .andExpect(jsonPath("$.summary.name").value("recorder"))
                .andExpect(jsonPath("$.summary.scopes").value(Matchers.containsInAnyOrder("glossary:read",
                        "campaigns:read")))
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(created, "$.token");

        mockMvc.perform(get("/api/accounts/me/api-tokens").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prefix").value(secret.substring(0, 8)))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test
    void rejectsUnknownScopesAndEmptyScopeSets() throws Exception {
        String auth = authHeader();

        mockMvc.perform(post("/api/accounts/me/api-tokens").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"scopes\":[\"accounts:admin\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts/me/api-tokens").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"scopes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aTokenCannotManageTokensOrReachEndpointsThatDoNotNameItsScope() throws Exception {
        String auth = authHeader();
        String secret = createToken(auth);
        String tokenAuth = "Bearer " + secret;

        mockMvc.perform(get("/api/accounts/me/api-tokens").header(HttpHeaders.AUTHORIZATION, tokenAuth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, tokenAuth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/worlds").header(HttpHeaders.AUTHORIZATION, tokenAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRevokedTokenIsRejectedAndRevocationIsScopedToTheOwner() throws Exception {
        String auth = authHeader();
        String otherAuth = authHeader();
        String created = mockMvc.perform(post("/api/accounts/me/api-tokens")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.summary.id");
        String secret = JsonPath.read(created, "$.token");

        mockMvc.perform(delete("/api/accounts/me/api-tokens/{id}", id).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/accounts/me/api-tokens/{id}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + secret))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/me/api-tokens").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/accounts/me/api-tokens/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnknownSecretIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer cot_nope"))
                .andExpect(status().isUnauthorized());
    }

    private String createToken(String auth) throws Exception {
        String created = mockMvc.perform(post("/api/accounts/me/api-tokens")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.token");
    }
}
