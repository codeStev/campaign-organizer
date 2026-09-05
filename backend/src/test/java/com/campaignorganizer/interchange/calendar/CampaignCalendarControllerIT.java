package com.campaignorganizer.interchange.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** End-to-end coverage of the campaign .ics export endpoints (ADR-0108). */
class CampaignCalendarControllerIT extends AbstractIntegrationTest {

    @Test
    void calendarFeedAndDownloadRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/calendar-feed", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/calendar-feed/regenerate",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/calendar.ics", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrCreateIsIdempotentUntilRegenerated() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId, "Shadows over Duskharrow");

        String first = getToken(auth, worldId, campaignId);
        String second = getToken(auth, worldId, campaignId);
        assertThat(second).isEqualTo(first);

        String regenerated = regenerateToken(auth, worldId, campaignId);
        assertThat(regenerated).isNotEqualTo(first);

        // The old token is now invalid on the public feed.
        mockMvc.perform(get("/api/calendar/{t}.ics", first))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/calendar/{t}.ics", regenerated))
                .andExpect(status().isOk());
    }

    @Test
    void downloadReturnsAnIcsCalendarWithTheCampaignsSessions() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId, "Shadows over Duskharrow");
        createSession(auth, worldId, campaignId, "Session 1: Arrival", LocalDate.of(2026, 8, 1));
        createSession(auth, worldId, campaignId, "Undated prep session", null);

        String body = mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/calendar.ics", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/calendar")))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .contains("BEGIN:VCALENDAR")
                .contains("SUMMARY:Shadows over Duskharrow — Session 1: Arrival")
                .doesNotContain("Undated prep session");
    }

    @Test
    void publicFeedServesTheSameCalendarWithoutAuthentication() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId, "Shadows over Duskharrow");
        createSession(auth, worldId, campaignId, "Session 1: Arrival", LocalDate.of(2026, 8, 1));
        String token = getToken(auth, worldId, campaignId);

        mockMvc.perform(get("/api/calendar/{t}.ics", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/calendar")))
                .andExpect(content().string(
                        containsString("SUMMARY:Shadows over Duskharrow — Session 1: Arrival")));
    }

    @Test
    void publicFeedWithUnknownTokenIs404() throws Exception {
        mockMvc.perform(get("/api/calendar/{t}.ics", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private String createCampaign(String auth, String worldId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void createSession(String auth, String worldId, String campaignId, String title, LocalDate date)
            throws Exception {
        String dateJson = date == null ? "null" : "\"" + date + "\"";
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"date\":" + dateJson + "}"))
                .andExpect(status().isCreated());
    }

    private String getToken(String auth, String worldId, String campaignId) throws Exception {
        String response = mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/calendar-feed", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private String regenerateToken(String auth, String worldId, String campaignId) throws Exception {
        String response = mockMvc.perform(post(
                        "/api/worlds/{w}/campaigns/{c}/calendar-feed/regenerate", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
