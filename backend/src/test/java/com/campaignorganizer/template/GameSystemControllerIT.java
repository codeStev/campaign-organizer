package com.campaignorganizer.template;

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

/** World-independent game systems (ADR-0094). */
class GameSystemControllerIT extends AbstractIntegrationTest {

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();

        String created = mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D&D 5e\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("D&D 5e"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/game-systems/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("D&D 5e"));

        mockMvc.perform(put("/api/game-systems/{s}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D&D 5e (revised)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("D&D 5e (revised)"));

        mockMvc.perform(get("/api/game-systems").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("D&D 5e (revised)")));

        mockMvc.perform(delete("/api/game-systems/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/game-systems/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void roundTripsTaglineColorAndNotes() throws Exception {
        String auth = authHeader();

        String created = mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vaesen (details test)\",\"tagline\":\"Gothic horror mystery\","
                                + "\"color\":\"#2c3e50\",\"notes\":\"SRD: https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagline").value("Gothic horror mystery"))
                .andExpect(jsonPath("$.color").value("#2c3e50"))
                .andExpect(jsonPath("$.notes").value("SRD: https://example.com"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/game-systems/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Gothic horror mystery"))
                .andExpect(jsonPath("$.color").value("#2c3e50"));
    }

    @Test
    void rejectsDuplicateNameCaseInsensitively() throws Exception {
        String auth = authHeader();
        mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pirate Borg\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PIRATE BORG\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteFailsWhileAGlobalTemplateStillReferencesIt() throws Exception {
        String auth = authHeader();
        String systemId = JsonPath.read(mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mothership\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Warden\",\"kind\":\"CHARACTER\",\"systemId\":\"" + systemId
                                + "\",\"sections\":[]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/game-systems/{s}", systemId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    @Test
    void notFoundForUnknownSystem() throws Exception {
        String auth = authHeader();
        mockMvc.perform(get("/api/game-systems/{s}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
