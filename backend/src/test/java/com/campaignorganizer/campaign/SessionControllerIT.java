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

class SessionControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String campaignId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String c = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\"}"))
                .andReturn().getResponse().getContentAsString();
        campaignId = JsonPath.read(c, "$.id");
    }

    private void addSession(String json) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void ordersByNumberThenDateWithNullsLast() throws Exception {
        setup();
        addSession("{\"title\":\"Session Two\",\"sessionNumber\":2}");
        addSession("{\"title\":\"Prologue\"}"); // no number -> trails
        addSession("{\"title\":\"Session One\",\"sessionNumber\":1,\"date\":\"2026-01-05\"}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Session One"))
                .andExpect(jsonPath("$[0].date").value("2026-01-05"))
                .andExpect(jsonPath("$[1].title").value("Session Two"))
                .andExpect(jsonPath("$[2].title").value("Prologue"));
    }

    @Test
    void updatesAndDeletesSession() throws Exception {
        setup();
        String created = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/sessions/{s}", worldId, campaignId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Final\",\"sessionNumber\":5,\"summary\":\"They won.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.sessionNumber").value(5))
                .andExpect(jsonPath("$.summary").value("They won."));

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/sessions/{s}", worldId, campaignId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void sessionUnderUnknownCampaignIs404() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingCampaignRemovesItsSessions() throws Exception {
        setup();
        addSession("{\"title\":\"S1\",\"sessionNumber\":1}");

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
