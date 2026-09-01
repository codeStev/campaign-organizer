package com.campaignorganizer.campaign;

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

/** Integration tests for the campaign roster (ADR-0091): whole-set replace, guest flag. */
class CampaignRosterControllerIT extends AbstractIntegrationTest {

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

    private String createPlayer(String name) throws Exception {
        String p = mockMvc.perform(post("/api/worlds/{w}/players", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(p, "$.id");
    }

    private String rosterUrl() {
        return "/api/worlds/{w}/campaigns/{c}/roster";
    }

    @Test
    void requiresAuthentication() throws Exception {
        setup();
        mockMvc.perform(get(rosterUrl(), worldId, campaignId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsAnEmptyRosterBeforeTheFirstPut() throws Exception {
        setup();

        mockMvc.perform(get(rosterUrl(), worldId, campaignId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void putReplacesTheWholeRosterAndDenormalizesNames() throws Exception {
        setup();
        String danaId = createPlayer("Dana");
        String remyId = createPlayer("Remy");

        mockMvc.perform(put(rosterUrl(), worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId + "\",\"guest\":false},"
                                + "{\"playerId\":\"" + remyId + "\",\"guest\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get(rosterUrl(), worldId, campaignId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.playerId=='" + danaId + "')].name").value("Dana"))
                .andExpect(jsonPath("$[?(@.playerId=='" + danaId + "')].guest").value(false))
                .andExpect(jsonPath("$[?(@.playerId=='" + remyId + "')].guest").value(true));

        // A second PUT with just Dana, promoted to guest, fully replaces the set.
        mockMvc.perform(put(rosterUrl(), worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + danaId + "\",\"guest\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guest").value(true));
    }

    @Test
    void rejectsAnUnknownPlayer() throws Exception {
        setup();

        mockMvc.perform(put(rosterUrl(), worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"playerId\":\"" + java.util.UUID.randomUUID()
                                + "\",\"guest\":false}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCampaignReturns404() throws Exception {
        setup();

        mockMvc.perform(get(rosterUrl(), worldId, java.util.UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
