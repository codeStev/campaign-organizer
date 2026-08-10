package com.campaignorganizer.world;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class WorldControllerIT extends AbstractIntegrationTest {

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supportsFullWorldLifecycle() throws Exception {
        String auth = "Bearer " + obtainToken();

        // Create
        String created = mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aethelgard\",\"description\":\"A misty realm\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value("Aethelgard"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // List contains it
        mockMvc.perform(get("/api/worlds").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // Fetch by id
        mockMvc.perform(get("/api/worlds/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("A misty realm"));

        // Update
        mockMvc.perform(put("/api/worlds/" + id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aethelgard Reborn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aethelgard Reborn"))
                .andExpect(jsonPath("$.description").doesNotExist());

        // Delete
        mockMvc.perform(delete("/api/worlds/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Gone
        mockMvc.perform(get("/api/worlds/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidWorld() throws Exception {
        String auth = "Bearer " + obtainToken();

        mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"no name\"}"))
                .andExpect(status().isBadRequest());
    }

    private String obtainToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
