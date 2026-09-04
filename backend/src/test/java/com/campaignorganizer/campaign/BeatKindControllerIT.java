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

/** Integration tests for the world-scoped beat kind catalog (ADR-0101). */
class BeatKindControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/beat-kinds", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Combat\",\"color\":\"#c0392b\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Combat"))
                .andExpect(jsonPath("$.color").value("#c0392b"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/beat-kinds", worldId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/worlds/{w}/beat-kinds/{k}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Skirmish\",\"color\":\"#ff0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Skirmish"))
                .andExpect(jsonPath("$.color").value("#ff0000"));

        mockMvc.perform(delete("/api/worlds/{w}/beat-kinds/{k}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/beat-kinds/{k}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBeatKindWithoutName() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateNameInSameWorldCaseInsensitively() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Combat\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"COMBAT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void sameNameIsAllowedInDifferentWorlds() throws Exception {
        String auth = authHeader();
        String worldA = createWorld(auth);
        String worldB = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldA)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Combat\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/worlds/{w}/beat-kinds", worldB)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Combat\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void notFoundForUnknownBeatKind() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(get("/api/worlds/{w}/beat-kinds/{k}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
