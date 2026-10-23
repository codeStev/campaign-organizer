package com.campaignorganizer.interchange.companion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** HTTP-level coverage for the companion campaign list and campaign glossary (ADR-0120). */
class CompanionControllerIT extends AbstractIntegrationTest {

    @Test
    void bothEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/companion/campaigns")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/companion/campaigns/{c}/glossary", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsCampaignsAcrossAllOwnedWorldsAndNeverAnotherAccountsOnes() throws Exception {
        String auth = authHeader();
        String worldA = createWorld(auth);
        String worldB = createWorld(auth);
        String campA = createCampaign(auth, worldA, "Echos des Chaos");
        String campB = createCampaign(auth, worldB, "Second Campaign");
        String strangerAuth = authHeader();
        createCampaign(strangerAuth, createWorld(strangerAuth), "Not Yours");

        mockMvc.perform(get("/api/companion/campaigns").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(Matchers.containsInAnyOrder(campA, campB)))
                .andExpect(jsonPath("$[*].name").value(Matchers.not(Matchers.hasItem("Not Yours"))))
                .andExpect(jsonPath("$[?(@.id == '" + campA + "')].worldId").value(Matchers.contains(worldA)));
    }

    @Test
    void glossaryHoldsOnlyWhatIsTiedToTheCampaign() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId, "Echos des Chaos");
        String otherCampaignId = createCampaign(auth, worldId, "Another Campaign");
        String arcId = createArc(auth, worldId, campaignId);

        String eckart = createArticle(auth, worldId, "Eckart", "CHARACTER");
        String rabenfels = createArticle(auth, worldId, "Rabenfels", "LOCATION");
        String notes = createArticle(auth, worldId, "Session notes", "GENERIC");
        createArticle(auth, worldId, "Unlinked Town", "LOCATION");
        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, eckart)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"Gastwirt Eckart\"]}"))
                .andExpect(status().isOk());

        String sharedStatblock = createStatblock(auth, worldId, "Goblin Spearman", null);
        createStatblock(auth, worldId, "Nachtschatten", campaignId);
        createStatblock(auth, worldId, "Secret Dragon", otherCampaignId);
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tavern\",\"articleIds\":[\"" + eckart + "\",\"" + rabenfels + "\",\""
                                + notes + "\"],\"statblockIds\":[\"" + sharedStatblock + "\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/companion/campaigns/{c}/glossary", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignName").value("Echos des Chaos"))
                .andExpect(jsonPath("$.terms[*].name").value(Matchers.contains(
                        "Eckart", "Goblin Spearman", "Nachtschatten", "Rabenfels")))
                .andExpect(jsonPath("$.terms[?(@.name == 'Eckart')].kind").value(Matchers.contains("character")))
                .andExpect(jsonPath("$.terms[?(@.name == 'Eckart')].aliases[0]").value(Matchers.contains("Gastwirt Eckart")))
                .andExpect(jsonPath("$.terms[?(@.name == 'Goblin Spearman')].kind").value(Matchers.contains("creature")))
                .andExpect(jsonPath("$.terms[?(@.name == 'Rabenfels')].kind").value(Matchers.contains("location")))
                .andExpect(jsonPath("$.terms[*].name").value(Matchers.not(Matchers.hasItems(
                        "Session notes", "Unlinked Town", "Secret Dragon"))));
    }

    @Test
    void anotherAccountsCampaignAndAnUnknownCampaignAreNotFound() throws Exception {
        String auth = authHeader();
        String campaignId = createCampaign(auth, createWorld(auth), "Private");
        String strangerAuth = authHeader();

        mockMvc.perform(get("/api/companion/campaigns/{c}/glossary", campaignId)
                        .header(HttpHeaders.AUTHORIZATION, strangerAuth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/companion/campaigns/{c}/glossary", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private String createCampaign(String auth, String worldId, String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createArc(String auth, String worldId, String campaignId) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Arc\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createArticle(String auth, String worldId, String title, String template) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"template\":\"" + template + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createStatblock(String auth, String worldId, String name, String campaignId) throws Exception {
        String campaign = campaignId == null ? "" : ",\"campaignId\":\"" + campaignId + "\"";
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"" + campaign + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }
}
