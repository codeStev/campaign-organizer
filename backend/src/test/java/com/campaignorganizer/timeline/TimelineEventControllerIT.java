package com.campaignorganizer.timeline;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class TimelineEventControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String timelineId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String tl = mockMvc.perform(post("/api/worlds/{w}/timelines", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\"}"))
                .andReturn().getResponse().getContentAsString();
        timelineId = JsonPath.read(tl, "$.id");
    }

    private void addEvent(String json) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void ordersEventsChronologicallyWithNullsFirst() throws Exception {
        setup();
        // Inserted out of order; year-only event should sort before dated ones in the same year.
        addEvent("{\"title\":\"War\",\"year\":1000,\"month\":3}");
        addEvent("{\"title\":\"Founding\",\"year\":500}");
        addEvent("{\"title\":\"Dawn\",\"year\":1000}");

        mockMvc.perform(get("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Founding"))
                .andExpect(jsonPath("$[1].title").value("Dawn"))
                .andExpect(jsonPath("$[2].title").value("War"));
    }

    @Test
    void supportsNegativeYears() throws Exception {
        setup();
        addEvent("{\"title\":\"Cataclysm\",\"year\":-2000}");
        addEvent("{\"title\":\"Now\",\"year\":0}");

        mockMvc.perform(get("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cataclysm"))
                .andExpect(jsonPath("$[0].year").value(-2000));
    }

    @Test
    void linksEventToArticle() throws Exception {
        setup();
        String articleId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"The Great War\"}"))
                        .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"War begins\",\"year\":1000,\"articleId\":\"" + articleId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleId").value(articleId));
    }

    @Test
    void rejectsEventWithoutYear() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No date\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsForeignArticle() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"year\":1,\"articleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
