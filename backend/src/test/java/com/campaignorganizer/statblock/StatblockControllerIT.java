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

class StatblockControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/statblocks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDeleteWithJsonbStats() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\",\"stats\":{\"AC\":15,\"HP\":7,\"speed\":\"30 ft\"},"
                                + "\"notes\":\"Nimble escape.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goblin"))
                .andExpect(jsonPath("$.stats.AC").value(15))
                .andExpect(jsonPath("$.stats.speed").value("30 ft"))
                .andExpect(jsonPath("$.notes").value("Nimble escape."))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/statblocks/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.HP").value(7));

        mockMvc.perform(put("/api/worlds/{w}/statblocks/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Boss\",\"stats\":{\"AC\":17,\"HP\":21}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goblin Boss"))
                .andExpect(jsonPath("$.stats.HP").value(21));

        mockMvc.perform(delete("/api/worlds/{w}/statblocks/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsForeignArticleLink() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Orphan\",\"articleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignsAndFiltersByCampaign() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String camp = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Boss Run\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ancient Dragon\",\"campaignId\":\"" + camp + "\",\"stats\":{\"CR\":24}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(camp));
        mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("campaignId", camp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ancient Dragon"));
    }

    @Test
    void campaignFilterIncludesSharedStatblocksReferencedByBeats() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String camp = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Wars\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String arc = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, camp)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Arc\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        // A shared statblock (no campaignId) referenced only by a beat in the campaign.
        String shared = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Spearman\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, camp, arc)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ambush\",\"statblockIds\":[\"" + shared + "\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("campaignId", camp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("Goblin Spearman")));
    }

    @Test
    void rejectsForeignCampaign() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"campaignId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
