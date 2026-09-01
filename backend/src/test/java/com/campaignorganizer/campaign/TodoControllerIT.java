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

/** Covers both {@link com.campaignorganizer.campaign.adapter.todo.in.web.CampaignTodoController}
 * and {@link com.campaignorganizer.campaign.adapter.todo.in.web.SessionTodoController} (ADR-0092). */
class TodoControllerIT extends AbstractIntegrationTest {

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

    @Test
    void addsListsUpdatesAndDeletesAStandingTodo() throws Exception {
        setup();

        String created = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/todos", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Print handout X\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andReturn().getResponse().getContentAsString();
        String todoId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/todos", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Print handout X"));

        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/todos/{t}", worldId, campaignId, todoId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Print handout X\",\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/todos/{t}", worldId, campaignId, todoId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/todos", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addsAndListsASessionTodoWithoutItAppearingInStandingList() throws Exception {
        setup();

        String created = mockMvc.perform(post(
                        "/api/worlds/{w}/campaigns/{c}/sessions/{s}/todos", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Reskin the goblin statblock\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andReturn().getResponse().getContentAsString();
        String todoId = JsonPath.read(created, "$.id");

        mockMvc.perform(get(
                        "/api/worlds/{w}/campaigns/{c}/sessions/{s}/todos", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // The session todo must not leak into the standing (campaign-level) list.
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/todos", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // The shared update/delete route (nested only under the campaign) still works for it.
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/todos/{t}", worldId, campaignId, todoId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Reskin the goblin statblock\",\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));
    }

    @Test
    void rejectsSessionTodoForForeignSession() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions/{s}/todos", worldId, campaignId,
                        UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Should not persist\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankText() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/todos", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
