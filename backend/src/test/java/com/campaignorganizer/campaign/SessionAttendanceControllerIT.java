package com.campaignorganizer.campaign;

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

/**
 * Integration tests for per-session attendance (ADR-0091): roster
 * pre-population, whole-set replace, and character-sheet campaign scoping.
 */
class SessionAttendanceControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String campaignId;
    private String sessionId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String c = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chronicle\"}"))
                .andReturn().getResponse().getContentAsString();
        campaignId = JsonPath.read(c, "$.id");
        String s = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"sessionNumber\":1}"))
                .andReturn().getResponse().getContentAsString();
        sessionId = JsonPath.read(s, "$.id");
    }

    private String createPlayer(String name) throws Exception {
        String p = mockMvc.perform(post("/api/worlds/{w}/players", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(p, "$.id");
    }

    private void addToRoster(String... playerIds) throws Exception {
        StringBuilder entries = new StringBuilder();
        for (String id : playerIds) {
            if (entries.length() > 0) {
                entries.append(",");
            }
            entries.append("{\"playerId\":\"").append(id).append("\",\"guest\":false}");
        }
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/roster", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[" + entries + "]}"))
                .andExpect(status().isOk());
    }

    /** Creates a CHARACTER-kind field template, then a filled sheet against it, scoped to campaignId (nullable). */
    private String createCharacterSheet(String name, String campaignIdOrNull) throws Exception {
        String template = mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","kind":"CHARACTER","system":"homebrew","sections":[]}
                                """))
                .andReturn().getResponse().getContentAsString();
        String templateId = JsonPath.read(template, "$.id");
        String campaignField = campaignIdOrNull == null ? "" : ",\"campaignId\":\"" + campaignIdOrNull + "\"";
        String sheet = mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"templateId\":\"" + templateId + "\""
                                + campaignField + "}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(sheet, "$.id");
    }

    private String attendanceUrl() {
        return "/api/worlds/{w}/campaigns/{c}/sessions/{s}/attendance";
    }

    @Test
    void requiresAuthentication() throws Exception {
        setup();
        mockMvc.perform(get(attendanceUrl(), worldId, campaignId, sessionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPrePopulatesFromTheRosterDefaultingToPresent() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);

        mockMvc.perform(get(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].playerId").value(danaId))
                .andExpect(jsonPath("$[0].name").value("Dana"))
                .andExpect(jsonPath("$[0].present").value(true))
                .andExpect(jsonPath("$[0].characterId").doesNotExist());
    }

    @Test
    void putRecordsAbsenceAndCharacterPlayed() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);
        String characterId = createCharacterSheet("Kessa", campaignId);

        mockMvc.perform(put(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId
                                + "\",\"present\":true,\"characterId\":\"" + characterId + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].present").value(true))
                .andExpect(jsonPath("$[0].characterId").value(characterId))
                .andExpect(jsonPath("$[0].characterName").value("Kessa"));

        mockMvc.perform(get(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$[0].characterId").value(characterId));
    }

    @Test
    void acceptsASharedCharacterSheetWithNoCampaign() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);
        String sharedCharacterId = createCharacterSheet("Wanderer", null);

        mockMvc.perform(put(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId
                                + "\",\"present\":true,\"characterId\":\"" + sharedCharacterId + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].characterId").value(sharedCharacterId));
    }

    @Test
    void rejectsACharacterSheetScopedToAnotherCampaign() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);
        String otherCampaign = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\"}"))
                .andReturn().getResponse().getContentAsString();
        String otherCampaignId = JsonPath.read(otherCampaign, "$.id");
        String foreignCharacterId = createCharacterSheet("Stranger", otherCampaignId);

        mockMvc.perform(put(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId
                                + "\",\"present\":true,\"characterId\":\"" + foreignCharacterId + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUnknownCharacterSheet() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);

        mockMvc.perform(put(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId
                                + "\",\"present\":true,\"characterId\":\"" + UUID.randomUUID() + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attendanceRowSurvivesRemovalFromTheRoster() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        addToRoster(danaId);
        mockMvc.perform(put(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId + "\",\"present\":false}]}"))
                .andExpect(status().isOk());

        // Roster PUT with an empty set removes Dana from the roster entirely.
        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/roster", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}"))
                .andExpect(status().isOk());

        // Her historical attendance for this session must still be visible.
        mockMvc.perform(get(attendanceUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].playerId").value(danaId))
                .andExpect(jsonPath("$[0].present").value(false));
    }

    @Test
    void unknownSessionReturns404() throws Exception {
        setup();

        mockMvc.perform(get(attendanceUrl(), worldId, campaignId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
