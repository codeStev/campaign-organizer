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

class BeatControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String campaignId;
    private String arcId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        campaignId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        arcId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Main Arc\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String articleId(String title) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void addsBeatLinkedToArticleAndOrders() throws Exception {
        setup();
        String villain = articleId("The Red Dragon");

        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Confront the villain\",\"position\":2,\"articleId\":\""
                                + villain + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleId").value(villain))
                .andExpect(jsonPath("$.done").value(false));

        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The hook\",\"position\":1,\"done\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("The hook"))
                .andExpect(jsonPath("$[0].done").value(true))
                .andExpect(jsonPath("$[1].title").value("Confront the villain"));
    }

    @Test
    void updatesBeatToneDoneAndSessionLink() throws Exception {
        setup();
        // A session in this campaign to link the beat to.
        String sessionId = JsonPath.read(mockMvc.perform(post(
                        "/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"sessionNumber\":1}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        String beatId = JsonPath.read(mockMvc.perform(post(
                        "/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft beat\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats/{b}",
                        worldId, campaignId, arcId, beatId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Resolved beat\",\"done\":true,\"sessionId\":\""
                                + sessionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Resolved beat"))
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.sessionId").value(sessionId));
    }

    @Test
    void rejectsBeatWithForeignSession() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"sessionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBeatWithForeignArticle() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"articleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingArticleNullsBeatLink() throws Exception {
        setup();
        String npc = articleId("Sildar");
        String beat = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Meet Sildar\",\"articleId\":\"" + npc + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonPath.read(beat, "$.id");

        // Deleting the article leaves the beat but nulls its link (ON DELETE SET NULL).
        mockMvc.perform(delete("/api/worlds/{w}/articles/{a}", worldId, npc)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Meet Sildar"))
                .andExpect(jsonPath("$[0].articleId").doesNotExist());
    }
}
