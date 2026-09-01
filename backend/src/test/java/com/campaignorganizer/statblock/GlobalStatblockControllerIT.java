package com.campaignorganizer.statblock;

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

/** The world-independent, system-scoped global statblock catalog (ADR-0096). */
class GlobalStatblockControllerIT extends AbstractIntegrationTest {

    private String createGameSystem(String auth, String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createCampaign(String auth, String worldId) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"The Society\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();
        String systemId = createGameSystem(auth, "D&D 5e (global-statblock-crud)");

        String created = mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Grunt\",\"systemId\":\"" + systemId
                                + "\",\"stats\":{\"HP\":7,\"AC\":15},\"notes\":\"Nasty little thing\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goblin Grunt"))
                .andExpect(jsonPath("$.systemId").value(systemId))
                .andExpect(jsonPath("$.stats.HP").value(7))
                .andExpect(jsonPath("$.notes").value("Nasty little thing"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/statblocks/global/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goblin Grunt"));

        mockMvc.perform(put("/api/statblocks/global/{s}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Grunt (revised)\",\"systemId\":\"" + systemId
                                + "\",\"stats\":{\"HP\":9},\"notes\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goblin Grunt (revised)"))
                .andExpect(jsonPath("$.stats.HP").value(9));

        mockMvc.perform(delete("/api/statblocks/global/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/statblocks/global/{s}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsStatblockWithoutSystem() throws Exception {
        String auth = authHeader();
        mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No system\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsATemplateFromADifferentSystem() throws Exception {
        String auth = authHeader();
        String systemA = createGameSystem(auth, "System A (cross-system)");
        String systemB = createGameSystem(auth, "System B (cross-system)");
        String templateB = JsonPath.read(mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Monster\",\"kind\":\"STATBLOCK\",\"systemId\":\"" + systemB
                                + "\",\"sections\":[]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\",\"systemId\":\"" + systemA + "\",\"globalTemplateId\":\""
                                + templateB + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersBySystemId() throws Exception {
        String auth = authHeader();
        String systemA = createGameSystem(auth, "System A (filter)");
        String systemB = createGameSystem(auth, "System B (filter)");
        mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A-monster\",\"systemId\":\"" + systemA + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B-monster\",\"systemId\":\"" + systemB + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/statblocks/global?systemId=" + systemA)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("A-monster")))
                .andExpect(jsonPath("$[*].name",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("B-monster"))));
    }

    @Test
    void importCopiesIntoACampaignWithNoLiveLinkBack() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId);
        String systemId = createGameSystem(auth, "Vaesen (import)");

        String created = mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Adult Red Dragon\",\"systemId\":\"" + systemId
                                + "\",\"stats\":{\"HP\":256},\"notes\":\"Breathes fire\"}"))
                .andReturn().getResponse().getContentAsString();
        String globalId = JsonPath.read(created, "$.id");

        String imported = mockMvc.perform(post("/api/statblocks/global/{s}/import", globalId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"worldId\":\"" + worldId + "\",\"campaignId\":\"" + campaignId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Adult Red Dragon"))
                .andExpect(jsonPath("$.worldId").value(worldId))
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andExpect(jsonPath("$.stats.HP").value(256))
                .andExpect(jsonPath("$.notes").value("Breathes fire"))
                .andReturn().getResponse().getContentAsString();
        String worldStatblockId = JsonPath.read(imported, "$.id");

        // Editing the copy must not affect the catalog original (copy-on-import,
        // not a live reference, ADR-0096).
        mockMvc.perform(put("/api/worlds/{w}/statblocks/{s}", worldId, worldStatblockId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Scarred Red Dragon\",\"campaignId\":\"" + campaignId
                                + "\",\"stats\":{\"HP\":200}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/statblocks/global/{s}", globalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adult Red Dragon"))
                .andExpect(jsonPath("$.stats.HP").value(256));

        // Deleting the catalog original afterward must not fail or touch the copy
        // (nothing FKs to a GlobalStatblock, since import never leaves a live link).
        mockMvc.perform(delete("/api/statblocks/global/{s}", globalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/statblocks/{s}", worldId, worldStatblockId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Scarred Red Dragon"));
    }

    @Test
    void importRejectsMissingCampaignId() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String systemId = createGameSystem(auth, "Vaesen (import-no-campaign)");
        String globalId = JsonPath.read(mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vaettir\",\"systemId\":\"" + systemId + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/statblocks/global/{s}/import", globalId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"worldId\":\"" + worldId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importRejectsAnUnknownCatalogEntry() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String campaignId = createCampaign(auth, worldId);

        mockMvc.perform(post("/api/statblocks/global/{s}/import", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"worldId\":\"" + worldId + "\",\"campaignId\":\"" + campaignId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void notFoundForUnknownStatblock() throws Exception {
        String auth = authHeader();
        mockMvc.perform(get("/api/statblocks/global/{s}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
