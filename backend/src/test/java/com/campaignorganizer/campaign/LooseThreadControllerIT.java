package com.campaignorganizer.campaign;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class LooseThreadControllerIT extends AbstractIntegrationTest {

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
        sessionId = JsonPath.read(mockMvc.perform(post(
                        "/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"sessionNumber\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String basePath() {
        return "/api/worlds/{w}/campaigns/{c}/sessions/{s}/loose-threads";
    }

    @Test
    void quickAddsListsUpdatesStatusAndDeletes() throws Exception {
        setup();

        String created = mockMvc.perform(post(basePath(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"A stranger left a coin on the table\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andReturn().getResponse().getContentAsString();
        String threadId = JsonPath.read(created, "$.id");

        mockMvc.perform(get(basePath(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("A stranger left a coin on the table"));

        mockMvc.perform(put(basePath() + "/{t}", worldId, campaignId, sessionId, threadId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"A stranger left a coin on the table\",\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(delete(basePath() + "/{t}", worldId, campaignId, sessionId, threadId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(basePath(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsThreadForForeignSession() throws Exception {
        setup();
        mockMvc.perform(post(basePath(), worldId, campaignId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Should not persist\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankText() throws Exception {
        setup();
        mockMvc.perform(post(basePath(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
