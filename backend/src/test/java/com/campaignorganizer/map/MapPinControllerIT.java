package com.campaignorganizer.map;

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

class MapPinControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String mapId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String mediaId = uploadImage(auth, worldId);
        String map = mockMvc.perform(post("/api/worlds/{w}/maps", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Overworld\",\"mediaId\":\"" + mediaId + "\"}"))
                .andReturn().getResponse().getContentAsString();
        mapId = JsonPath.read(map, "$.id");
    }

    private String createArticle() throws Exception {
        String art = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Capital City\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(art, "$.id");
    }

    @Test
    void addsPinLinkedToArticleAndLists() throws Exception {
        setup();
        String articleId = createArticle();

        mockMvc.perform(post("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":0.5,\"y\":0.25,\"label\":\"Capital\",\"layer\":\"cities\","
                                + "\"articleId\":\"" + articleId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.x").value(0.5))
                .andExpect(jsonPath("$.layer").value("cities"))
                .andExpect(jsonPath("$.articleId").value(articleId));

        mockMvc.perform(get("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updatesPin() throws Exception {
        setup();
        String pin = mockMvc.perform(post("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":0.1,\"y\":0.1,\"label\":\"old\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String pinId = JsonPath.read(pin, "$.id");

        mockMvc.perform(put("/api/worlds/{w}/maps/{m}/pins/{p}", worldId, mapId, pinId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":0.7,\"y\":0.8,\"label\":\"new\",\"layer\":\"ruins\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x").value(0.7))
                .andExpect(jsonPath("$.label").value("new"))
                .andExpect(jsonPath("$.layer").value("ruins"));
    }

    @Test
    void rejectsOutOfRangeCoordinates() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":2.0,\"y\":0.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsForeignArticle() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":0.1,\"y\":0.1,\"articleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingMapRemovesItsPins() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":0.1,\"y\":0.1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/worlds/{w}/maps/{m}", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Map (and its pins) are gone.
        mockMvc.perform(get("/api/worlds/{w}/maps/{m}/pins", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
