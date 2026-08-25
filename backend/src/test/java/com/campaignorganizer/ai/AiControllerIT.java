package com.campaignorganizer.ai;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Draft and provider-test endpoints over HTTP. Both real provider adapters are
 * replaced by mocks — these tests must never touch the network; the adapters'
 * own unit tests cover HTTP mapping, the settings IT covers the unconfigured
 * path with the real beans. The bean names are explicit because two
 * {@link TextGenerationPort} beans share the type; without them Spring cannot
 * pick which definition to replace.
 */
class AiControllerIT extends AbstractIntegrationTest {

    @MockitoBean(name = "groqTextGenerationAdapter")
    private TextGenerationPort groq;

    @MockitoBean(name = "openRouterTextGenerationAdapter")
    private TextGenerationPort openRouter;

    private String draftBody() {
        return "{\"instructions\":\"a gruff dockmaster\",\"existingContent\":\"\"}";
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/ai/draft-article-text", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankInstructions_is400() throws Exception {
        stubProviderIds();
        mockMvc.perform(post("/api/worlds/{w}/ai/draft-article-text", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructions\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void successfulDraft_returnsTextAndProvider() throws Exception {
        stubProviderIds();
        Mockito.when(groq.configured()).thenReturn(true);
        Mockito.when(groq.generate(anyString(), anyString(), anyString()))
                .thenReturn(new com.campaignorganizer.ai.domain.DraftResult("Salt on the wind.", "groq"));

        mockMvc.perform(post("/api/worlds/{w}/ai/draft-article-text", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Salt on the wind."))
                .andExpect(jsonPath("$.provider").value("groq"));
    }

    @Test
    void everyProviderFailing_is503ProblemJson() throws Exception {
        stubProviderIds();
        Mockito.when(groq.configured()).thenReturn(true);
        Mockito.when(openRouter.configured()).thenReturn(true);
        Mockito.when(groq.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("down"));
        Mockito.when(openRouter.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("down"));

        mockMvc.perform(post("/api/worlds/{w}/ai/draft-article-text", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("AI unavailable"));
    }

    @Test
    void providerTest_reportsSuccessAsData() throws Exception {
        stubProviderIds();
        Mockito.when(groq.configured()).thenReturn(true);
        Mockito.when(groq.generate(anyString(), anyString(), anyString()))
                .thenReturn(new com.campaignorganizer.ai.domain.DraftResult("OK", "groq"));

        mockMvc.perform(post("/api/ai/settings/{id}/test", "groq")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void providerTest_reportsFailureAsData_notAsServerError() throws Exception {
        stubProviderIds();
        Mockito.when(groq.configured()).thenReturn(true);
        Mockito.when(groq.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("401 unauthorized"));

        mockMvc.perform(post("/api/ai/settings/{id}/test", "groq")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("401")));
    }

    @Test
    void providerTest_unknownProviderIs404() throws Exception {
        stubProviderIds();
        mockMvc.perform(post("/api/ai/settings/{id}/test", "nope")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    /** The service map is keyed by providerId(); both mocks must identify themselves. */
    private void stubProviderIds() {
        Mockito.lenient().when(groq.providerId()).thenReturn("groq");
        Mockito.lenient().when(openRouter.providerId()).thenReturn("openrouter");
        Mockito.lenient().when(groq.defaultModel()).thenReturn("groq-default");
        Mockito.lenient().when(openRouter.defaultModel()).thenReturn("openrouter-default");
        Mockito.lenient().when(openRouter.configured()).thenReturn(false);
    }
}
