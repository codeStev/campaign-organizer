package com.campaignorganizer.campaign;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ClockControllerIT extends AbstractIntegrationTest {

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

    private String addClock(String json) throws Exception {
        return mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/clocks", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void createsOrdersUpdatesAndDeletes() throws Exception {
        setup();
        addClock("{\"title\":\"Second\",\"segments\":[],\"position\":2}");
        String first = addClock("{\"title\":\"First\",\"segments\":"
                + "[{\"filled\":false},{\"filled\":false,\"title\":\"Alarm\"}],\"position\":1}");
        String firstId = JsonPath.read(first, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/clocks", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[0].segments.length()").value(2))
                .andExpect(jsonPath("$[0].segments[1].title").value("Alarm"))
                .andExpect(jsonPath("$[1].title").value("Second"));

        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/clocks/{k}", worldId, campaignId, firstId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"segments\":"
                                + "[{\"filled\":true},{\"filled\":false,\"title\":\"Alarm\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].filled").value(true));

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/clocks/{k}", worldId, campaignId, firstId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void defaultsPositionToZero() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/clocks", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fresh\",\"segments\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.segments.length()").value(0));
    }
}
