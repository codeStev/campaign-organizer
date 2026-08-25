package com.campaignorganizer.tables;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RollTableControllerIT extends AbstractIntegrationTest {

    @Test
    void createsListsGetsUpdatesAndDeletesATable() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String tableId = UUID.randomUUID().toString();

        String created = mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Weather","description":"Daily weather","diceExpression":"2d6",
                                 "entries":[{"minResult":2,"maxResult":7,"body":"Rain over [[Greyhaven]]"},
                                            {"minResult":8,"maxResult":12,"body":"Clear skies"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Weather"))
                .andExpect(jsonPath("$.diceExpression").value("2d6"))
                .andExpect(jsonPath("$.minResult").value(2))
                .andExpect(jsonPath("$.maxResult").value(12))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        tableId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        // Entry ids are assigned server-side.
        mockMvc.perform(get("/api/worlds/{w}/roll-tables/{t}", worldId, tableId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].id").isNotEmpty())
                .andExpect(jsonPath("$.entries[0].minResult").value(2))
                .andExpect(jsonPath("$.entries[0].body").value("Rain over [[Greyhaven]]"));

        mockMvc.perform(get("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/worlds/{w}/roll-tables/{t}", worldId, tableId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Season","diceExpression":"1d4",
                                 "entries":[{"minResult":1,"maxResult":2,"body":"Spring"},
                                            {"minResult":3,"maxResult":4,"body":"Autumn"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Season"))
                .andExpect(jsonPath("$.maxResult").value(4))
                .andExpect(jsonPath("$.entries.length()").value(2));

        mockMvc.perform(delete("/api/worlds/{w}/roll-tables/{t}", worldId, tableId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidDiceExpression() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bad","diceExpression":"not dice",
                                 "entries":[{"body":"x"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOverlappingEntryRanges() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Overlap","diceExpression":"1d10",
                                 "entries":[{"minResult":1,"maxResult":5,"body":"A"},
                                            {"minResult":5,"maxResult":6,"body":"B"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"diceExpression\":\"1d6\",\"entries\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTableReturns404AndForeignWorldIsIsolated() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/roll-tables/{t}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/worlds/{w}/roll-tables/{t}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/roll-tables", worldId))
                .andExpect(status().isUnauthorized());
    }
}
