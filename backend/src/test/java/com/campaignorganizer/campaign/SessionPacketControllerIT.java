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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SessionPacketControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String campaignId;
    private String arcId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        campaignId = create("/api/worlds/" + worldId + "/campaigns", "{\"name\":\"Chronicle\"}");
        arcId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs",
                "{\"title\":\"Main Arc\"}");
    }

    /** POST a body and return the created entity's id. */
    private String create(String path, String body) throws Exception {
        return JsonPath.read(response(post(path), body), "$.id");
    }

    /** Perform a request with a JSON body and return the raw response body. */
    private String response(MockHttpServletRequestBuilder req, String body) throws Exception {
        return mockMvc.perform(req.header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aggregatesBeatsArticlesAndStatblocks() throws Exception {
        setup();
        String npc = create("/api/worlds/" + worldId + "/articles",
                "{\"title\":\"Sildar\",\"body\":\"A knight.\"}");
        String place = create("/api/worlds/" + worldId + "/articles", "{\"title\":\"Phandalin\"}");
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\",\"sessionNumber\":1}");

        // A beat scheduled into the session, linking the NPC and the place.
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Meet Sildar\",\"sessionId\":\"" + sessionId + "\",\"articleIds\":[\""
                        + npc + "\",\"" + place + "\"]}");
        // A beat in another session must NOT appear.
        String otherSession = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 2\"}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Later\",\"sessionId\":\"" + otherSession + "\"}");

        // A campaign-scoped statblock (e.g. a boss for this campaign).
        create("/api/worlds/" + worldId + "/statblocks",
                "{\"name\":\"Klarg\",\"campaignId\":\"" + campaignId + "\"}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.title").value("Session 1"))
                .andExpect(jsonPath("$.campaignName").value("Chronicle"))
                .andExpect(jsonPath("$.beats.length()").value(1))
                .andExpect(jsonPath("$.beats[0].title").value("Meet Sildar"))
                .andExpect(jsonPath("$.beats[0].arcTitle").value("Main Arc"))
                .andExpect(jsonPath("$.articles[*].title", Matchers.hasItems("Sildar", "Phandalin")))
                .andExpect(jsonPath("$.articles[?(@.title == 'Sildar')].bodyHtml")
                        .value(Matchers.hasItem(Matchers.containsString("A knight."))))
                .andExpect(jsonPath("$.statblocks[*].name", Matchers.hasItem("Klarg")));
    }

    @Test
    void exposesParentArticleIdForNestingInThePrintTree() throws Exception {
        setup();
        String parent = create("/api/worlds/" + worldId + "/articles", "{\"title\":\"Phandalin\"}");
        String child = create("/api/worlds/" + worldId + "/articles",
                "{\"title\":\"Stonehill Inn\",\"parentArticleId\":\"" + parent + "\"}");
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Arrive\",\"sessionId\":\"" + sessionId + "\",\"articleIds\":[\""
                        + parent + "\",\"" + child + "\"]}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[?(@.title == 'Phandalin')].parentArticleId")
                        .value(Matchers.hasItem(Matchers.nullValue())))
                .andExpect(jsonPath("$.articles[?(@.title == 'Stonehill Inn')].parentArticleId")
                        .value(Matchers.hasItem(parent)));
    }

    @Test
    void includesMapsLinkedViaBeatArticles() throws Exception {
        setup();
        String place = create("/api/worlds/" + worldId + "/articles", "{\"title\":\"Phandalin\"}");
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Arrive\",\"sessionId\":\"" + sessionId + "\",\"articleIds\":[\"" + place + "\"]}");

        // A map with an (unlabeled) pin linking the beat's article.
        String mediaId = uploadImage(auth, worldId);
        String mapId = create("/api/worlds/" + worldId + "/maps",
                "{\"name\":\"Sword Coast\",\"mediaId\":\"" + mediaId + "\"}");
        create("/api/worlds/" + worldId + "/maps/" + mapId + "/pins",
                "{\"x\":0.4,\"y\":0.6,\"articleId\":\"" + place + "\"}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maps[*].name", Matchers.hasItem("Sword Coast")))
                // The pin has no label, so it falls back to the linked article's title.
                .andExpect(jsonPath("$.maps[0].pins[0].label").value("Phandalin"));
    }

    @Test
    void includesRollTablesAndCardDecksLinkedViaBeats() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        String weather = create("/api/worlds/" + worldId + "/roll-tables",
                "{\"title\":\"Weather\",\"diceExpression\":\"1d2\",\"entries\":"
                        + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Rain over [[Phandalin]]\"},"
                        + "{\"minResult\":2,\"maxResult\":2,\"body\":\"Sun\"}]}");
        String omens = create("/api/worlds/" + worldId + "/card-decks",
                "{\"title\":\"Omens\",\"cards\":[{\"title\":\"The Tower\",\"body\":\"Disaster.\"}]}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"On the road\",\"sessionId\":\"" + sessionId + "\","
                        + "\"tableIds\":[\"" + weather + "\"],\"deckIds\":[\"" + omens + "\"]}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollTables.length()").value(1))
                .andExpect(jsonPath("$.rollTables[0].title").value("Weather"))
                .andExpect(jsonPath("$.rollTables[0].diceExpression").value("1d2"))
                .andExpect(jsonPath("$.rollTables[0].entries.length()").value(2))
                // Outcome text renders through the article pipeline (wiki-links resolved).
                .andExpect(jsonPath("$.rollTables[0].entries[0].bodyHtml")
                        .value(Matchers.containsString("Rain over")))
                .andExpect(jsonPath("$.cardDecks.length()").value(1))
                .andExpect(jsonPath("$.cardDecks[0].title").value("Omens"))
                .andExpect(jsonPath("$.cardDecks[0].cards[0].title").value("The Tower"))
                .andExpect(jsonPath("$.cardDecks[0].cards[0].bodyHtml").value(Matchers.containsString("Disaster.")));
    }

    @Test
    void chainsNestedTablesAndDecksIntoThePacketAndCutsCycles() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        // A is the beat's direct reference; B's row chains A, then A is updated
        // to chain B back (a stored cycle), and deck C's cards both chain B.
        String tableA = create("/api/worlds/" + worldId + "/roll-tables",
                "{\"title\":\"Encounter\",\"diceExpression\":\"1d1\",\"entries\":"
                        + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Bandits\"}]}");
        String tableB = create("/api/worlds/" + worldId + "/roll-tables",
                "{\"title\":\"Loot\",\"diceExpression\":\"1d1\",\"entries\":"
                        + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Coins\","
                        + "\"nestedTableIds\":[\"" + tableA + "\"]}]}");
        mockMvc.perform(put("/api/worlds/{w}/roll-tables/{t}", worldId, tableA)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Encounter\",\"diceExpression\":\"1d1\",\"entries\":"
                                + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Bandits\","
                                + "\"nestedTableIds\":[\"" + tableB + "\"]}]}"))
                .andExpect(status().isOk());
        String deckC = create("/api/worlds/" + worldId + "/card-decks",
                "{\"title\":\"Twists\",\"cards\":[{\"title\":\"Ambush\",\"body\":\"More foes\","
                        + "\"nestedTableIds\":[\"" + tableB + "\"]},"
                        + "{\"body\":\"Second twist\",\"nestedTableIds\":[\"" + tableB + "\"]}]}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"On the road\",\"sessionId\":\"" + sessionId + "\","
                        + "\"tableIds\":[\"" + tableA + "\"],\"deckIds\":[\"" + deckC + "\"]}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                // Both chained tables arrive, each exactly once despite the cycle.
                .andExpect(jsonPath("$.rollTables.length()").value(2))
                .andExpect(jsonPath("$.rollTables[*].title",
                        Matchers.containsInAnyOrder("Encounter", "Loot")))
                .andExpect(jsonPath("$.cardDecks.length()").value(1))
                .andExpect(jsonPath("$.cardDecks[0].title").value("Twists"));
    }

    @Test
    void omitsTablesAndDecksWhenBeatsReferenceNone() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Quiet night\",\"sessionId\":\"" + sessionId + "\"}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollTables.length()").value(0))
                .andExpect(jsonPath("$.cardDecks.length()").value(0));
    }

    @Test
    void includesCampaignClocksUnconditionallyWithoutFillState() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        // No beat references this clock at all - it must still appear, campaign-wide.
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/clocks",
                "{\"title\":\"Doom\",\"segments\":"
                        + "[{\"filled\":true},{\"filled\":false,\"title\":\"Alarm\",\"description\":\"Guards\"}]}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clocks.length()").value(1))
                .andExpect(jsonPath("$.clocks[0].title").value("Doom"))
                .andExpect(jsonPath("$.clocks[0].segments.length()").value(2))
                .andExpect(jsonPath("$.clocks[0].segments[1].title").value("Alarm"))
                // Stateless by design (ADR-0084): no fill flag leaks into the packet.
                .andExpect(jsonPath("$.clocks[0].segments[0].filled").doesNotExist())
                .andExpect(jsonPath("$.clocks[0].segments[1].filled").doesNotExist());
    }

    @Test
    void printsArticleReferencedFromBeatTableAndDeckExactlyOnce() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        // Referenced three ways: directly on the beat, from a table outcome,
        // and from a deck card — plus a table-only article.
        String tavern = create("/api/worlds/" + worldId + "/articles", "{\"title\":\"The Rusty Tankard\"}");
        String wizard = create("/api/worlds/" + worldId + "/articles", "{\"title\":\"Elara the Grey\"}");
        String weather = create("/api/worlds/" + worldId + "/roll-tables",
                "{\"title\":\"Weather\",\"diceExpression\":\"1d1\",\"entries\":"
                        + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Storm hits [[The Rusty Tankard]]\"}]}");
        String omens = create("/api/worlds/" + worldId + "/card-decks",
                "{\"title\":\"Omens\",\"cards\":[{\"body\":\"A stranger: [[Elara the Grey]] and "
                        + "[[The Rusty Tankard]] again\"}]}");
        create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/arcs/" + arcId + "/beats",
                "{\"title\":\"Night out\",\"sessionId\":\"" + sessionId + "\","
                        + "\"articleIds\":[\"" + tavern + "\"],"
                        + "\"tableIds\":[\"" + weather + "\"],\"deckIds\":[\"" + omens + "\"]}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                // The tavern appears once despite three reference paths; the wizard
                // joins via the deck card; both resolve through [[wiki-links]].
                .andExpect(jsonPath("$.articles[*].title",
                        Matchers.containsInAnyOrder("The Rusty Tankard", "Elara the Grey")))
                .andExpect(jsonPath("$.articles[0].id").value(tavern));
    }

    @Test
    void resolvesCheatSheetFragmentsFreshAndFlagsDanglingOnes() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");
        String statblockId = create("/api/worlds/" + worldId + "/statblocks",
                "{\"name\":\"Klarg\",\"campaignId\":\"" + campaignId + "\"}");
        String tableResponse = response(post("/api/worlds/" + worldId + "/roll-tables"),
                "{\"title\":\"Weather\",\"diceExpression\":\"1d1\",\"entries\":"
                        + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Storm over [[Phandalin]]\"}]}");
        String tableId = JsonPath.read(tableResponse, "$.id");
        String entryId = JsonPath.read(tableResponse, "$.entries[0].id");
        String deckResponse = response(post("/api/worlds/" + worldId + "/card-decks"),
                "{\"title\":\"Omens\",\"cards\":[{\"title\":\"The Tower\",\"body\":\"Disaster.\"}]}");
        String deckId = JsonPath.read(deckResponse, "$.id");
        String cardId = JsonPath.read(deckResponse, "$.cards[0].id");
        // A reference must exist to be saved (ADR-0071); it only dangles once
        // the statblock is deleted afterward.
        String doomedStatblockId = create("/api/worlds/" + worldId + "/statblocks",
                "{\"name\":\"Doomed\",\"campaignId\":\"" + campaignId + "\"}");

        response(put("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/sessions/" + sessionId + "/cheat-sheet"),
                "{\"fragments\":["
                        + "{\"type\":\"FREEFORM\",\"text\":\"Check the harbor door first\"},"
                        + "{\"type\":\"STATBLOCK\",\"statblockId\":\"" + statblockId + "\"},"
                        + "{\"type\":\"TABLE_ROW\",\"tableId\":\"" + tableId + "\",\"entryId\":\"" + entryId + "\"},"
                        + "{\"type\":\"DECK_CARD\",\"deckId\":\"" + deckId + "\",\"cardId\":\"" + cardId + "\"},"
                        + "{\"type\":\"STATBLOCK\",\"statblockId\":\"" + doomedStatblockId + "\"}"
                        + "]}");
        mockMvc.perform(delete("/api/worlds/" + worldId + "/statblocks/" + doomedStatblockId)
                        .header(HttpHeaders.AUTHORIZATION, auth));

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cheatSheet.fragments.length()").value(5))
                .andExpect(jsonPath("$.cheatSheet.fragments[0].type").value("FREEFORM"))
                .andExpect(jsonPath("$.cheatSheet.fragments[0].text").value("Check the harbor door first"))
                .andExpect(jsonPath("$.cheatSheet.fragments[0].missing").value(false))
                .andExpect(jsonPath("$.cheatSheet.fragments[1].type").value("STATBLOCK"))
                .andExpect(jsonPath("$.cheatSheet.fragments[1].statblock.name").value("Klarg"))
                .andExpect(jsonPath("$.cheatSheet.fragments[1].missing").value(false))
                .andExpect(jsonPath("$.cheatSheet.fragments[2].type").value("TABLE_ROW"))
                .andExpect(jsonPath("$.cheatSheet.fragments[2].tableTitle").value("Weather"))
                .andExpect(jsonPath("$.cheatSheet.fragments[2].tableEntry.bodyHtml")
                        .value(Matchers.containsString("Storm over")))
                .andExpect(jsonPath("$.cheatSheet.fragments[3].type").value("DECK_CARD"))
                .andExpect(jsonPath("$.cheatSheet.fragments[3].deckTitle").value("Omens"))
                .andExpect(jsonPath("$.cheatSheet.fragments[3].deckCard.title").value("The Tower"))
                .andExpect(jsonPath("$.cheatSheet.fragments[3].deckCard.bodyHtml")
                        .value(Matchers.containsString("Disaster.")))
                .andExpect(jsonPath("$.cheatSheet.fragments[4].type").value("STATBLOCK"))
                .andExpect(jsonPath("$.cheatSheet.fragments[4].missing").value(true))
                .andExpect(jsonPath("$.cheatSheet.fragments[4].statblock").doesNotExist());
    }

    @Test
    void omitsCheatSheetWhenSessionHasNoneSaved() throws Exception {
        setup();
        String sessionId = create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions",
                "{\"title\":\"Session 1\"}");

        mockMvc.perform(get("/api/worlds/{w}/campaigns/{c}/sessions/{s}/packet",
                        worldId, campaignId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cheatSheet").doesNotExist());
    }
}
