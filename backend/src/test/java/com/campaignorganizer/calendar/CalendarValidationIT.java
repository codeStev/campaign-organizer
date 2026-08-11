package com.campaignorganizer.calendar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Verifies that a timeline's calendar constrains its events' dates (ADR-0020). */
class CalendarValidationIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String timelineId;

    private void setupWithCalendar() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String cal = mockMvc.perform(post("/api/worlds/{w}/calendars", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reckoning\",\"months\":"
                                + "[{\"name\":\"Frostfall\",\"days\":30},{\"name\":\"Sunreach\",\"days\":31}]}"))
                .andReturn().getResponse().getContentAsString();
        String calendarId = JsonPath.read(cal, "$.id");

        String tl = mockMvc.perform(post("/api/worlds/{w}/timelines", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\",\"calendarId\":\"" + calendarId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calendarId").value(calendarId))
                .andReturn().getResponse().getContentAsString();
        timelineId = JsonPath.read(tl, "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions addEvent(String json) throws Exception {
        return mockMvc.perform(post("/api/worlds/{w}/timelines/{t}/events", worldId, timelineId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void acceptsDateWithinCalendar() throws Exception {
        setupWithCalendar();
        addEvent("{\"title\":\"Feast\",\"year\":1000,\"month\":2,\"day\":31}")
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDayBeyondMonthLength() throws Exception {
        setupWithCalendar();
        // Frostfall has 30 days.
        addEvent("{\"title\":\"Bad\",\"year\":1000,\"month\":1,\"day\":31}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMonthBeyondCount() throws Exception {
        setupWithCalendar();
        // Only two months exist.
        addEvent("{\"title\":\"Bad\",\"year\":1000,\"month\":3}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTimelineWithForeignCalendar() throws Exception {
        String a = authHeader();
        String world = createWorld(a);
        mockMvc.perform(post("/api/worlds/{w}/timelines", world)
                        .header(HttpHeaders.AUTHORIZATION, a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T\",\"calendarId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
