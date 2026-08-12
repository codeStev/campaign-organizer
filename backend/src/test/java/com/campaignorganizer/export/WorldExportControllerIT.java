package com.campaignorganizer.export;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class WorldExportControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/export", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownWorldIs404() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/export", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportsWorldWithItsEntities() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Goblin\",\"body\":\"<p>Small and mean.</p>\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main Campaign\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/export", worldId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString(".json")))
                .andExpect(jsonPath("$.exportVersion").value(1))
                .andExpect(jsonPath("$.world.id").value(worldId))
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].title").value("Goblin"))
                .andExpect(jsonPath("$.campaigns.length()").value(1))
                .andExpect(jsonPath("$.statblocks.length()").value(0));
    }
}
