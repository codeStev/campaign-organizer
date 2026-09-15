package com.campaignorganizer.interchange.overview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Account-wide landing page stats end-to-end (issue #68): real persistence. */
class GlobalOverviewControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    void aggregatesUpcomingSessionsAndCampaignsNeedingAttentionAcrossWorlds() throws Exception {
        String auth = authHeader();
        String worldAId = createWorld(auth, "World A");
        String worldBId = createWorld(auth, "World B");

        String scheduledCampaignId = createCampaign(auth, worldAId, "Scheduled", "ACTIVE");
        createSession(auth, worldAId, scheduledCampaignId, "Next up", LocalDate.now().plusDays(5));

        createCampaign(auth, worldBId, "Idle planned", "PLANNED");
        createCampaign(auth, worldBId, "On hiatus", "ON_HIATUS");

        mockMvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingSessions.length()").value(1))
                .andExpect(jsonPath("$.upcomingSessions[0].title").value("Next up"))
                .andExpect(jsonPath("$.upcomingSessions[0].worldName").value("World A"))
                .andExpect(jsonPath("$.upcomingSessions[0].campaignName").value("Scheduled"))
                .andExpect(jsonPath("$.campaignsNeedingAttention.length()").value(1))
                .andExpect(jsonPath("$.campaignsNeedingAttention[0].campaignName").value("Idle planned"))
                .andExpect(jsonPath("$.campaignsNeedingAttention[0].worldName").value("World B"));
    }

    @Test
    void neverMixesTwoAccountsWorlds() throws Exception {
        String authA = authHeader();
        createWorld(authA, "Account A's world");

        String authB = authHeader();

        mockMvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingSessions.length()").value(0))
                .andExpect(jsonPath("$.campaignsNeedingAttention.length()").value(0));
    }

    private String createWorld(String auth, String name) throws Exception {
        String response = mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createCampaign(String auth, String worldId, String name, String status) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String campaignId = JsonPath.read(response, "$.id");
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
        return campaignId;
    }

    private void createSession(String auth, String worldId, String campaignId, String title, LocalDate date)
            throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"date\":\"" + date + "\"}"))
                .andExpect(status().isCreated());
    }
}
