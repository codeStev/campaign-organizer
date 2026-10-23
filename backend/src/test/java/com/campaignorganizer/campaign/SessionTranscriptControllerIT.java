package com.campaignorganizer.campaign;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort.LineInput;
import com.campaignorganizer.shared.domain.ValidationException;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Integration tests for session transcripts (ADR-0121): the GM's read and delete endpoints, and the
 * published write port the companion export uses (the HTTP write path has its own test).
 */
class SessionTranscriptControllerIT extends AbstractIntegrationTest {

    @Autowired
    private SessionTranscriptWritePort writePort;

    private String auth;
    private String worldId;
    private String campaignId;
    private String sessionId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        campaignId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        sessionId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"sessionNumber\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** Calls the published write port as the account, the way an authenticated request would. */
    private void asOwner(Runnable call) throws Exception {
        UUID accountId = UUID.fromString(JsonPath.read(mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountId, null, List.of()));
        try {
            call.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void replace(List<LineInput> lines) throws Exception {
        asOwner(() -> writePort.replace(UUID.fromString(campaignId), UUID.fromString(sessionId), lines));
    }

    private static List<LineInput> story() {
        return List.of(new LineInput(500, null, "NARRATION", "Der Wirt blickt auf."),
                new LineInput(2500, "Eckart", "IN_CHARACTER", "Willkommen im Raben!"));
    }

    @Test
    void requiresAuthenticationAndAnOwnedSession() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID())).andExpect(status().isUnauthorized());
        setup();
        String stranger = authHeader();

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId,
                        sessionId).header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSessionWithoutATranscriptHasAnEmptyView() throws Exception {
        setup();

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void readsBackTheLinesInOrderAndReplacementDoesNotAppend() throws Exception {
        setup();
        replace(story());
        replace(List.of(new LineInput(0, null, "NARRATION", "Neue Fassung.")));
        replace(story());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].kind").value("NARRATION"))
                .andExpect(jsonPath("$.lines[0].speaker").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.lines[1].speaker").value("Eckart"))
                .andExpect(jsonPath("$.lines[1].text").value("Willkommen im Raben!"));
    }

    @Test
    void theStoreRefusesNonStoryKindsAndKeepsWhatWasThereBefore() throws Exception {
        setup();
        replace(story());

        assertThatThrownBy(() -> replace(List.of(new LineInput(0, null, "OFF_TOPIC", "Wie war dein Urlaub?"))))
                .isInstanceOf(ValidationException.class);

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void deleteRemovesTheTranscriptAndIsRepeatable() throws Exception {
        setup();
        replace(story());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId,
                            sessionId).header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void deletingTheSessionDeletesItsTranscript() throws Exception {
        setup();
        replace(story());

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/sessions/{s}", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
