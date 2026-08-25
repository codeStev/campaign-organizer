package com.campaignorganizer.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Settings round-trip over HTTP with the real persistence ring (JSONB table).
 * The test environment has no API keys, which is exactly the interesting case:
 * both known providers must appear with their defaults and configured=false
 * (the state a fresh deployment reports before .env keys exist).
 */
class AiSettingsControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/ai/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void afterResetToDefaults_listsKnownProvidersUnconfigured() throws Exception {
        // Settings are global state persisted in the shared test database, so
        // the test resets to defaults itself instead of assuming a fresh table.
        String body = """
                {"providers":[
                  {"providerId":"groq","model":null},
                  {"providerId":"openrouter","model":null}
                ]}
                """;
        mockMvc.perform(put("/api/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ai/settings").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].providerId").value("groq"))
                .andExpect(jsonPath("$[0].defaultModel").value("llama-3.3-70b-versatile"))
                .andExpect(jsonPath("$[0].model").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[0].configured").value(false))
                .andExpect(jsonPath("$[1].providerId").value("openrouter"))
                .andExpect(jsonPath("$[1].priority").value(1));
    }

    @Test
    void updatePersistsModelAndPriorityOrder() throws Exception {
        String body = """
                {"providers":[
                  {"providerId":"openrouter","model":"deepseek/deepseek-r1:free"},
                  {"providerId":"groq","model":null}
                ]}
                """;
        mockMvc.perform(put("/api/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value("openrouter"))
                .andExpect(jsonPath("$[0].priority").value(0));

        // A second GET reads it back from the database, proving persistence.
        mockMvc.perform(get("/api/ai/settings").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value("openrouter"))
                .andExpect(jsonPath("$[0].model").value("deepseek/deepseek-r1:free"));
    }
}
