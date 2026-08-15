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
    void addsBeatLinkedToMultipleArticlesAndOrders() throws Exception {
        setup();
        String villain = articleId("The Red Dragon");
        String place = articleId("Waterdeep");

        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Confront the villain\",\"position\":2,\"articleIds\":[\""
                                + villain + "\",\"" + place + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleIds.length()").value(2))
                .andExpect(jsonPath("$.articleIds", org.hamcrest.Matchers.hasItems(villain, place)))
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

    private String statblockId(String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void linksStatblocksToBeat() throws Exception {
        setup();
        String spearman = statblockId("Goblin Spearman");
        String archer = statblockId("Goblin Archer");

        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Goblin ambush\",\"statblockIds\":[\""
                                + spearman + "\",\"" + archer + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statblockIds.length()").value(2))
                .andExpect(jsonPath("$.statblockIds", org.hamcrest.Matchers.hasItems(spearman, archer)));
    }

    @Test
    void rejectsBeatWithForeignStatblock() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"statblockIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isBadRequest());
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
                        .content("{\"title\":\"Bad\",\"articleIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingArticleRemovesItFromBeatLinks() throws Exception {
        setup();
        String npc = articleId("Sildar");
        String place = articleId("Neverwinter");
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Meet Sildar\",\"articleIds\":[\"" + npc + "\",\"" + place + "\"]}"))
                .andExpect(status().isCreated());

        // Deleting one linked article removes only that link; the beat survives.
        mockMvc.perform(delete("/api/worlds/{w}/articles/{a}", worldId, npc)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Meet Sildar"))
                .andExpect(jsonPath("$[0].articleIds.length()").value(1))
                .andExpect(jsonPath("$[0].articleIds[0]").value(place));
    }
}
