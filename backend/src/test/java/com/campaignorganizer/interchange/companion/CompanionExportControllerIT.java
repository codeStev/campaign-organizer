package com.campaignorganizer.interchange.companion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** HTTP-level coverage for the companion session, summary and transcript export (ADR-0121). */
class CompanionExportControllerIT extends AbstractIntegrationTest {

    private static final String LINES = "{\"lines\":[{\"offsetMs\":500,\"kind\":\"NARRATION\","
            + "\"text\":\"Der Wirt blickt auf.\"},{\"offsetMs\":2500,\"speaker\":\"Eckart\","
            + "\"kind\":\"IN_CHARACTER\",\"text\":\"Willkommen!\"}]}";

    private String auth;
    private String worldId;
    private String campaignId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        campaignId = createCampaign(auth, worldId);
    }

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        UUID c = UUID.randomUUID();
        UUID s = UUID.randomUUID();
        mockMvc.perform(get("/api/companion/campaigns/{c}/sessions", c)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/companion/campaigns/{c}/sessions", c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", c, s)
                .contentType(MediaType.APPLICATION_JSON).content("{\"summary\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", c, s)
                .contentType(MediaType.APPLICATION_JSON).content(LINES)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/companion/campaigns/{c}/sessions/{s}/transcript", c, s))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsASessionNumbersItAfterTheLastOneAndListsItWithoutText() throws Exception {
        setup();
        createSession(auth, "Session 1", "2026-10-16", null);
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Prep\",\"sessionNumber\":4,\"summary\":\"private recap\","
                                + "\"notes\":\"secret prep\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 2026-10-23\",\"date\":\"2026-10-23\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionNumber").value(5))
                .andExpect(jsonPath("$.hasSummary").value(false));

        mockMvc.perform(get("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.title == 'Prep')].hasSummary").value(Matchers.contains(true)))
                .andExpect(jsonPath("$[0].summary").doesNotExist())
                .andExpect(jsonPath("$[0].notes").doesNotExist());
    }

    @Test
    void theSummaryIsWrittenOnceAndNeverOverwritesTheGmsEditsBlindly() throws Exception {
        setup();
        String sessionId = createSession(auth, "Session 1", "2026-10-23", null);

        String firstWrite = mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId,
                        sessionId).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"Recap one with [[Eckart]].\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSummary").value(true))
                .andReturn().getResponse().getContentAsString();
        String updatedAt = JsonPath.read(firstWrite, "$.updatedAt");

        // Writing again without the time it last saw is refused...
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"Recap two\"}"))
                .andExpect(status().isConflict());
        // ...with the time it saw it goes through...
        String secondWrite = mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId,
                        sessionId).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"Recap two\",\"expectedUpdatedAt\":\"" + updatedAt + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newUpdatedAt = JsonPath.read(secondWrite, "$.updatedAt");

        // ...and once the GM edits the session in the organizer, that time is stale.
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/sessions/{s}", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"summary\":\"The GM rewrote this\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"Recap three\",\"expectedUpdatedAt\":\"" + newUpdatedAt + "\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$[0].summary").value("The GM rewrote this"));
    }

    @Test
    void theTranscriptIsReplacedAsAWholeAndReadableByTheGm() throws Exception {
        setup();
        String sessionId = createSession(auth, "Session 1", "2026-10-23", null);

        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"offsetMs\":0,\"kind\":\"NARRATION\",\"text\":\"old\"}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCount").value(2));

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[1].speaker").value("Eckart"));

        mockMvc.perform(delete("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void theServerRefusesPrivateTalkWhateverTheClientSends() throws Exception {
        setup();
        String sessionId = createSession(auth, "Session 1", "2026-10-23", null);
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isOk());

        for (String kind : new String[] {"META", "OFF_TOPIC", "UNCLEAR", "in_character"}) {
            mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                            .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lines\":[{\"offsetMs\":0,\"kind\":\"" + kind
                                    + "\",\"text\":\"Wie läuft dein Job?\"}]}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());

        // The earlier, valid transcript is untouched by the refused attempts.
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/transcript", worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void eachCapabilityNeedsItsOwnTokenScope() throws Exception {
        setup();
        String sessionId = createSession(auth, "Session 1", "2026-10-23", null);
        String sessionsOnly = "Bearer " + createToken(auth, "sessions:write");
        String transcriptsOnly = "Bearer " + createToken(auth, "transcripts:write");

        mockMvc.perform(get("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, sessionsOnly))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, sessionsOnly).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"Recap\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, sessionsOnly).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, transcriptsOnly).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, transcriptsOnly))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, transcriptsOnly))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, transcriptsOnly).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isNotFound());

        // A token can never reach the GM's own session endpoints.
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, sessionsOnly))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherAccountsCampaignIsNotFoundForEveryEndpoint() throws Exception {
        setup();
        String sessionId = createSession(auth, "Session 1", "2026-10-23", null);
        String stranger = authHeader();

        mockMvc.perform(get("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/companion/campaigns/{c}/sessions", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, stranger).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, stranger).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, stranger).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSessionOfAnotherCampaignIsNotFound() throws Exception {
        setup();
        String otherCampaign = createCampaign(auth, worldId);
        String sessionOfOther = createSessionIn(auth, otherCampaign, "Elsewhere");

        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/transcript", campaignId, sessionOfOther)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content(LINES))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/companion/campaigns/{c}/sessions/{s}/summary", campaignId, sessionOfOther)
                        .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    private String createCampaign(String authHeader, String world) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", world)
                        .header(HttpHeaders.AUTHORIZATION, authHeader).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Echos des Chaos\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createSession(String authHeader, String title, String date, Integer number) throws Exception {
        return createSessionIn(authHeader, campaignId, title, date, number);
    }

    private String createSessionIn(String authHeader, String campaign, String title) throws Exception {
        return createSessionIn(authHeader, campaign, title, null, null);
    }

    private String createSessionIn(String authHeader, String campaign, String title, String date, Integer number)
            throws Exception {
        String body = "{\"title\":\"" + title + "\""
                + (date == null ? "" : ",\"date\":\"" + date + "\"")
                + (number == null ? "" : ",\"sessionNumber\":" + number) + "}";
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaign)
                        .header(HttpHeaders.AUTHORIZATION, authHeader).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createToken(String authHeader, String... scopes) throws Exception {
        String scopeJson = Arrays.stream(scopes).map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        return JsonPath.read(mockMvc.perform(post("/api/accounts/me/api-tokens")
                        .header(HttpHeaders.AUTHORIZATION, authHeader).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"recorder\",\"scopes\":[" + scopeJson + "]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.token");
    }
}
