package com.campaignorganizer.tables;

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

class CardDeckControllerIT extends AbstractIntegrationTest {

    @Test
    void createsListsUpdatesAndDeletesADeck() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Omens","description":"Draw for foreshadowing",
                                 "cards":[{"title":"The Fool","body":"A new beginning. See [[Portals]]"},
                                          {"title":"The Tower","body":"Disaster strikes"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Omens"))
                .andExpect(jsonPath("$.cards.length()").value(2))
                .andExpect(jsonPath("$.cards[0].title").value("The Fool"))
                .andReturn().getResponse().getContentAsString();
        String deckId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/worlds/{w}/card-decks/{d}", worldId, deckId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Omens II","cards":[{"title":"Reversed","body":"Twist"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Omens II"))
                .andExpect(jsonPath("$.cards.length()").value(1));

        mockMvc.perform(delete("/api/worlds/{w}/card-decks/{d}", worldId, deckId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsCardWithoutBody() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Deck\",\"cards\":[{\"title\":\"No body\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"cards\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesNestedChains() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String table = mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Weather\",\"diceExpression\":\"1d1\",\"entries\":"
                                + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Rain\"}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String tableId = JsonPath.read(table, "$.id");

        // A card chaining an existing table round-trips.
        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Omens\",\"cards\":[{\"title\":\"The Tower\","
                                + "\"body\":\"Disaster\",\"nestedTableIds\":[\"" + tableId
                                + "\"]}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cards[0].nestedTableIds[0]").value(tableId));

        // Unknown nested ids are rejected.
        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"cards\":[{\"body\":\"x\",\""
                                + "nestedTableIds\":[\"" + UUID.randomUUID() + "\"]}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad deck ref\",\"cards\":[{\"body\":\"x\",\""
                                + "nestedDeckIds\":[\"" + UUID.randomUUID() + "\"]}]}"))
                .andExpect(status().isBadRequest());

        // A deck cannot nest itself.
        String deck = mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Plain\",\"cards\":[{\"body\":\"x\"}]}"))
                .andReturn().getResponse().getContentAsString();
        String deckId = JsonPath.read(deck, "$.id");
        mockMvc.perform(put("/api/worlds/{w}/card-decks/{d}", worldId, deckId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Plain\",\"cards\":[{\"body\":\"Self\",\""
                                + "nestedDeckIds\":[\"" + deckId + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDeckReturns404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/card-decks/{d}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/worlds/{w}/card-decks/{d}/duplicate", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicatesADeckWithFreshCardIdsAndRenamedTitle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String source = mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Omens","description":"Draw for foreshadowing",
                                 "cards":[{"title":"The Fool","body":"A new beginning"},
                                          {"title":"The Tower","body":"Disaster strikes"}]}
                                """))
                .andReturn().getResponse().getContentAsString();
        String sourceId = JsonPath.read(source, "$.id");
        String sourceCardId = JsonPath.read(source, "$.cards[0].id");

        mockMvc.perform(post("/api/worlds/{w}/card-decks/{d}/duplicate", worldId, sourceId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(sourceId)))
                .andExpect(jsonPath("$.title").value("Omens (copy)"))
                .andExpect(jsonPath("$.description").value("Draw for foreshadowing"))
                .andExpect(jsonPath("$.cards.length()").value(2))
                .andExpect(jsonPath("$.cards[0].title").value("The Fool"))
                .andExpect(jsonPath("$.cards[0].id").value(org.hamcrest.Matchers.not(sourceCardId)));

        mockMvc.perform(get("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
