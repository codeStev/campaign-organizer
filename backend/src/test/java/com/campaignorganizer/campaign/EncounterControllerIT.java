package com.campaignorganizer.campaign;

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

/** The persisted encounter builder (ADR-0097). */
class EncounterControllerIT extends AbstractIntegrationTest {

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

    private String statblockId(String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void createsListsUpdatesAndDeletes() throws Exception {
        setup();
        String goblin = statblockId("Goblin");

        String created = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin ambush\",\"notes\":\"watch the road\","
                                + "\"entries\":[{\"statblockId\":\"" + goblin + "\",\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goblin ambush"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].statblockId").value(goblin))
                .andExpect(jsonPath("$.entries[0].quantity").value(3))
                .andReturn().getResponse().getContentAsString();
        String encounterId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Goblin ambush"));

        mockMvc.perform(put("/api/worlds/{w}/campaigns/{c}/encounters/{e}", worldId, campaignId, encounterId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin ambush (revised)\",\"entries\":[{\"statblockId\":\""
                                + goblin + "\",\"quantity\":5}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goblin ambush (revised)"))
                .andExpect(jsonPath("$.entries[0].quantity").value(5));

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}/encounters/{e}", worldId, campaignId, encounterId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAnEncounterWithAForeignStatblock() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"entries\":[{\"statblockId\":\"" + UUID.randomUUID()
                                + "\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsAnyNumberOfStatblocksInOneEncounter() throws Exception {
        setup();
        String goblinA = statblockId("Goblin Grunt");
        String goblinB = statblockId("Goblin Archer");
        String goblinC = statblockId("Goblin Boss");

        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Warband\",\"entries\":["
                                + "{\"statblockId\":\"" + goblinA + "\",\"quantity\":4},"
                                + "{\"statblockId\":\"" + goblinB + "\",\"quantity\":2},"
                                + "{\"statblockId\":\"" + goblinC + "\",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entries.length()").value(3));
    }

    @Test
    void deletingAReferencedStatblockRemovesItsEncounterEntry() throws Exception {
        setup();
        String goblin = statblockId("Goblin");
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ambush\",\"entries\":[{\"statblockId\":\"" + goblin
                                + "\",\"quantity\":2}]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/worlds/{w}/statblocks/{s}", worldId, goblin)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/encounters", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entries.length()").value(0));
    }
}
