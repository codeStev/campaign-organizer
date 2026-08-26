package com.campaignorganizer.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Integration tests for the per-session cheat sheet (FR-37): singleton
 * upsert semantics and reference validation against statblocks, roll
 * tables and card decks.
 */
class CheatSheetControllerIT extends AbstractIntegrationTest {

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
        String s = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId,
                        campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session 1\",\"sessionNumber\":1}"))
                .andReturn().getResponse().getContentAsString();
        sessionId = JsonPath.read(s, "$.id");
    }

    /** Seeds a statblock, a roll table and a card deck; returns {statblockId, tableId, entryId, deckId, cardId}. */
    private String[] seedReferencableContent() throws Exception {
        String statblock = mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\",\"stats\":{\"HP\":7}}"))
                .andReturn().getResponse().getContentAsString();
        String table = mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Weather","diceExpression":"2d6",
                                 "entries":[{"minResult":2,"maxResult":12,"body":"Rain"}]}
                                """))
                .andReturn().getResponse().getContentAsString();
        String deck = mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Omens","cards":[{"title":"The Tower",
                                 "body":"Disaster strikes"}]}
                                """))
                .andReturn().getResponse().getContentAsString();
        return new String[]{
                JsonPath.read(statblock, "$.id"),
                JsonPath.read(table, "$.id"),
                JsonPath.read(table, "$.entries[0].id"),
                JsonPath.read(deck, "$.id"),
                JsonPath.read(deck, "$.cards[0].id")};
    }

    private String sheetUrl() {
        return "/api/worlds/{w}/campaigns/{c}/sessions/{s}/cheat-sheet";
    }

    @Test
    void requiresAuthentication() throws Exception {
        setup();
        mockMvc.perform(get(sheetUrl(), worldId, campaignId, sessionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsAnIdNullSheetBeforeTheFirstPut() throws Exception {
        setup();

        mockMvc.perform(get(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.fragments.length()").value(0));
    }

    @Test
    void putUpsertsAndRoundTripsAllFourFragmentKinds() throws Exception {
        setup();
        String[] ids = seedReferencableContent();

        String saved = mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fragments":[
                                  {"type":"FREEFORM","text":"Check the harbor door first"},
                                  {"type":"STATBLOCK","statblockId":"%s"},
                                  {"type":"TABLE_ROW","tableId":"%s","entryId":"%s"},
                                  {"type":"DECK_CARD","deckId":"%s","cardId":"%s"}]}
                                """.formatted(ids[0], ids[1], ids[2], ids[3], ids[4])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fragments.length()").value(4))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fragments[0].text").value("Check the harbor door first"))
                .andExpect(jsonPath("$.fragments[1].statblockId").value(ids[0]))
                .andExpect(jsonPath("$.fragments[2].tableId").value(ids[1]))
                .andExpect(jsonPath("$.fragments[3].cardId").value(ids[4]));

        // A second PUT replaces the whole ordered list in place (same sheet id).
        String updated = mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"FREEFORM\",\"text\":\"New plan\"}]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(updated, "$.id"))
                .isEqualTo(JsonPath.<String>read(saved, "$.id"));
        mockMvc.perform(get(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.fragments.length()").value(1));

        mockMvc.perform(delete(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Deleting twice is fine; the sheet is gone either way.
        mockMvc.perform(delete(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.id").value(Matchers.nullValue()));
    }

    @Test
    void rejectsAnUnknownFragmentType() throws Exception {
        setup();

        mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"SONG\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAFreeformFragmentWithoutText() throws Exception {
        setup();

        mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"FREEFORM\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAStatblockFromAnotherWorld() throws Exception {
        setup();
        String otherAuth = authHeader();
        String otherWorld = createWorld(otherAuth);
        String foreign = mockMvc.perform(post("/api/worlds/{w}/statblocks", otherWorld)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Outsider\",\"stats\":{}}"))
                .andReturn().getResponse().getContentAsString();
        String foreignId = JsonPath.read(foreign, "$.id");

        mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"STATBLOCK\",\"statblockId\":\""
                                + foreignId + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAMissingTableRowAndDeckCard() throws Exception {
        setup();

        mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"TABLE_ROW\",\"tableId\":\""
                                + UUID.randomUUID() + "\",\"entryId\":\"" + UUID.randomUUID()
                                + "\"}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(sheetUrl(), worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fragments\":[{\"type\":\"DECK_CARD\",\"deckId\":\""
                                + UUID.randomUUID() + "\",\"cardId\":\"" + UUID.randomUUID()
                                + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSessionReturns404() throws Exception {
        setup();

        mockMvc.perform(get(sheetUrl(), worldId, campaignId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
