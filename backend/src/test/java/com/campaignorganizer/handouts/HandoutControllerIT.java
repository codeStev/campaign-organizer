package com.campaignorganizer.handouts;

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

class HandoutControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/handouts", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsUpdatesAndDeletesAHandout() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/handouts", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Wanted: the Harbor Ghost","preset":"POSTER",
                                 "body":"**500 gold** for capture alive."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Wanted: the Harbor Ghost"))
                .andExpect(jsonPath("$.preset").value("POSTER"))
                .andReturn().getResponse().getContentAsString();
        String handoutId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/handouts", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/worlds/{w}/handouts/{h}", worldId, handoutId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("**500 gold** for capture alive."));

        mockMvc.perform(put("/api/worlds/{w}/handouts/{h}", worldId, handoutId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Letter to the magistrate","preset":"LETTER",
                                 "body":"Dear sir,"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Letter to the magistrate"))
                .andExpect(jsonPath("$.preset").value("LETTER"));

        mockMvc.perform(delete("/api/worlds/{w}/handouts/{h}", worldId, handoutId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/handouts", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUnknownPreset() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/handouts", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Flyer\",\"preset\":\"NEON\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/handouts", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"preset\":\"PARCHMENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownHandoutReturns404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/handouts/{h}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
