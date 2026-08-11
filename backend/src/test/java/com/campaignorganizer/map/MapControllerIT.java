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

class MapControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/maps", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String mediaId = uploadImage(auth, worldId);

        String created = mockMvc.perform(post("/api/worlds/{w}/maps", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Overworld\",\"mediaId\":\"" + mediaId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Overworld"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/" + mediaId + "/content"))
                .andReturn().getResponse().getContentAsString();
        String mapId = JsonPath.read(created, "$.id");

        mockMvc.perform(put("/api/worlds/{w}/maps/{m}", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Overworld (rev)\",\"mediaId\":\"" + mediaId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Overworld (rev)"));

        mockMvc.perform(delete("/api/worlds/{w}/maps/{m}", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/maps/{m}", worldId, mapId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownImage() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/maps", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"mediaId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
