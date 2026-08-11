package com.campaignorganizer.calendar;

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

class CalendarControllerIT extends AbstractIntegrationTest {

    private static final String TWO_MONTHS =
            "\"months\":[{\"name\":\"Frostfall\",\"days\":30},{\"name\":\"Sunreach\",\"days\":31}]";

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/calendars", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/calendars", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reckoning\",\"daysPerWeek\":7," + TWO_MONTHS + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Reckoning"))
                .andExpect(jsonPath("$.months.length()").value(2))
                .andExpect(jsonPath("$.months[0].name").value("Frostfall"))
                .andExpect(jsonPath("$.months[1].days").value(31))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Update replaces months.
        mockMvc.perform(put("/api/worlds/{w}/calendars/{c}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reckoning\",\"months\":[{\"name\":\"Onemonth\",\"days\":10}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months.length()").value(1))
                .andExpect(jsonPath("$.months[0].name").value("Onemonth"));

        mockMvc.perform(delete("/api/worlds/{w}/calendars/{c}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsCalendarWithoutMonths() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/calendars", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empty\",\"months\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
