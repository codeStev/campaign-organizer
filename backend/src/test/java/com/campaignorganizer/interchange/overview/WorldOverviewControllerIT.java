package com.campaignorganizer.interchange.overview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** World overview stats end-to-end (FR-62, ADR-0102, ADR-0103): real persistence. */
class WorldOverviewControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/overview", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownWorldIs404() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/overview", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aggregatesArticleCountSessionsRunAndRecentlyEdited() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        createArticle(auth, worldId, "Lighthouse");
        createArticle(auth, worldId, "Harbor");

        String campaignId = createCampaign(auth, worldId);
        createSession(auth, worldId, campaignId, "Played it", LocalDate.now().minusDays(1));
        createSession(auth, worldId, campaignId, "Today's game", LocalDate.now());
        createSession(auth, worldId, campaignId, "Not yet", LocalDate.now().plusDays(30));
        createSession(auth, worldId, campaignId, "Unscheduled", null);

        mockMvc.perform(get("/api/worlds/{w}/overview", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleCount").value(2))
                .andExpect(jsonPath("$.sessionsRunCount").value(2))
                .andExpect(jsonPath("$.recentlyEdited.length()").value(2))
                .andExpect(jsonPath("$.recentlyEdited[0].title").value("Harbor"))
                .andExpect(jsonPath("$.nextSession.title").value("Not yet"))
                .andExpect(jsonPath("$.nextSession.campaignName").value("Main"))
                .andExpect(jsonPath("$.scheduledSessions.length()").value(3))
                .andExpect(jsonPath("$.scheduledSessions[0].title").value("Played it"))
                .andExpect(jsonPath("$.scheduledSessions[2].title").value("Not yet"))
                .andExpect(jsonPath("$.scheduledSessions[0].campaignName").value("Main"));
    }

    @Test
    void aggregatesOpenClocksAndOpenLooseThreads() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId);

        createClock(auth, worldId, campaignId, "Nearly there",
                "[{\"filled\":true},{\"filled\":true},{\"filled\":false}]");
        createClock(auth, worldId, campaignId, "Already full",
                "[{\"filled\":true},{\"filled\":true}]");

        String sessionId = createSession(auth, worldId, campaignId, "Session 1", LocalDate.now());
        createLooseThread(auth, worldId, campaignId, sessionId, "An open thread", "OPEN");
        createLooseThread(auth, worldId, campaignId, sessionId, "A resolved thread", "RESOLVED");

        mockMvc.perform(get("/api/worlds/{w}/overview", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openClocks.length()").value(1))
                .andExpect(jsonPath("$.openClocks[0].title").value("Nearly there"))
                .andExpect(jsonPath("$.openClocks[0].filledSegments").value(2))
                .andExpect(jsonPath("$.openClocks[0].totalSegments").value(3))
                .andExpect(jsonPath("$.openLooseThreads.length()").value(1))
                .andExpect(jsonPath("$.openLooseThreads[0].text").value("An open thread"));
    }

    private void createArticle(String auth, String worldId, String title) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated());
    }

    private String createCampaign(String auth, String worldId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createSession(String auth, String worldId, String campaignId, String title,
                                 LocalDate date) throws Exception {
        String dateJson = date == null ? "null" : "\"" + date + "\"";
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"date\":" + dateJson + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void createClock(String auth, String worldId, String campaignId, String title,
                             String segmentsJson) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/clocks", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"segments\":" + segmentsJson + "}"))
                .andExpect(status().isCreated());
    }

    private void createLooseThread(String auth, String worldId, String campaignId, String sessionId,
                                   String text, String status) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions/{s}/loose-threads",
                                worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isCreated());
    }
}
