package com.campaignorganizer.interchange.overview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Per-campaign dashboard stats end-to-end (issue #67): real persistence. */
class CampaignOverviewControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/overview", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownCampaignIs404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/overview", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void aggregatesNextSessionOpenClocksThreadsBeatsAndTodos() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId);

        createSession(auth, worldId, campaignId, "Played it", LocalDate.now().minusDays(1));
        String nextSessionId = createSession(auth, worldId, campaignId, "Not yet", LocalDate.now().plusDays(7));
        createSession(auth, worldId, campaignId, "Far off", LocalDate.now().plusDays(30));

        createClock(auth, worldId, campaignId, "Nearly there",
                "[{\"filled\":true,\"title\":\"Step 1\"},{\"filled\":false,\"title\":\"Step 2\"}]");
        createClock(auth, worldId, campaignId, "Already full", "[{\"filled\":true}]");

        createLooseThread(auth, worldId, campaignId, nextSessionId, "An open thread", "OPEN");
        createLooseThread(auth, worldId, campaignId, nextSessionId, "A resolved thread", "RESOLVED");

        String activeArcId = createArc(auth, worldId, campaignId, "The Rebellion", "ACTIVE");
        String plannedArcId = createArc(auth, worldId, campaignId, "Someday", "PLANNED");
        createBeat(auth, worldId, campaignId, activeArcId, "Open beat", false);
        createBeat(auth, worldId, campaignId, activeArcId, "Done beat", true);
        createBeat(auth, worldId, campaignId, plannedArcId, "Not yet relevant", false);

        createSessionTodo(auth, worldId, campaignId, nextSessionId, "Print handout");
        String doneTodoId = createSessionTodo(auth, worldId, campaignId, nextSessionId, "Already handled");
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/todos/{t}", worldId, campaignId, doneTodoId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Already handled\",\"done\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/overview", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextSession.title").value("Not yet"))
                .andExpect(jsonPath("$.openClocksNearFilling.length()").value(1))
                .andExpect(jsonPath("$.openClocksNearFilling[0].title").value("Nearly there"))
                .andExpect(jsonPath("$.openClocksNearFilling[0].nextUnfilledSegmentTitle").value("Step 2"))
                .andExpect(jsonPath("$.openLooseThreads.length()").value(1))
                .andExpect(jsonPath("$.openLooseThreads[0].text").value("An open thread"))
                .andExpect(jsonPath("$.openBeatsInActiveArcs.length()").value(1))
                .andExpect(jsonPath("$.openBeatsInActiveArcs[0].beatTitle").value("Open beat"))
                .andExpect(jsonPath("$.openBeatsInActiveArcs[0].arcTitle").value("The Rebellion"))
                .andExpect(jsonPath("$.nextSessionTodos.length()").value(1))
                .andExpect(jsonPath("$.nextSessionTodos[0].text").value("Print handout"));
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
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"date\":\"" + date + "\"}"))
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

    private String createArc(String auth, String worldId, String campaignId, String title, String status)
            throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void createBeat(String auth, String worldId, String campaignId, String arcId, String title,
                            boolean done) throws Exception {
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"done\":" + done + "}"))
                .andExpect(status().isCreated());
    }

    private String createSessionTodo(String auth, String worldId, String campaignId, String sessionId, String text)
            throws Exception {
        String response = mockMvc.perform(
                        post("/api/worlds/{w}/campaigns/{c}/sessions/{s}/todos", worldId, campaignId, sessionId)
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
