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
    void unknownDeckReturns404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/card-decks/{d}", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
