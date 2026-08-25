package com.campaignorganizer.usage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class UsageControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;

    private String article(String title, String body) throws Exception {
        String content = body == null
                ? "{\"title\":\"" + title + "\"}"
                : "{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}";
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/usages",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aggregatesBeatSheetAndWikiLinkUsages() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String goblin = article("Goblin", null);
        // Another article links to Goblin via a wiki-link.
        article("Bestiary", "<p>See [[Goblin]].</p>");

        // A campaign beat links Goblin.
        String campId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ashes\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String arcId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Arc\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ambush\",\"articleIds\":[\"" + goblin + "\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/usages", worldId, goblin)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usages[*].type", Matchers.hasItems("BEAT", "ARTICLE_LINK")))
                .andExpect(jsonPath("$.usages[?(@.type == 'BEAT')].campaignName")
                        .value(Matchers.hasItem("Ashes")));
    }

    @Test
    void articleListFiltersByCampaignUsage() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String used = article("Used NPC", null);
        article("Unused NPC", null);

        String campId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Run\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String arcId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Arc\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Beat\",\"articleIds\":[\"" + used + "\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("campaignId", campId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Used NPC"));
    }

    @Test
    void listsRollTableAndCardDeckUsages() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String tavern = article("The Rusty Tankard", null);

        mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bar Brawls\",\"diceExpression\":\"1d2\",\"entries\":"
                                + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Trouble at "
                                + "[[The Rusty Tankard]]\"},"
                                + "{\"minResult\":2,\"maxResult\":2,\"body\":\"Quiet night\"}]}"))
                .andExpect(status().isCreated());
        // An unrelated table must not show up.
        mockMvc.perform(post("/api/worlds/{w}/roll-tables", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Weather\",\"diceExpression\":\"1d1\",\"entries\":"
                                + "[{\"minResult\":1,\"maxResult\":1,\"body\":\"Rain\"}]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/worlds/{w}/card-decks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rumours\",\"cards\":[{\"body\":\"Heard of "
                                + "[[The Rusty Tankard]]?\"}]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/usages", worldId, tavern)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usages[?(@.type == 'ROLL_TABLE')].label")
                        .value(Matchers.hasItem("Roll table: Bar Brawls")))
                .andExpect(jsonPath("$.usages[?(@.type == 'CARD_DECK')].label")
                        .value(Matchers.hasItem("Card deck: Rumours")))
                // The unrelated Weather table does not appear.
                .andExpect(jsonPath("$.usages[*].label",
                        Matchers.not(Matchers.hasItem("Roll table: Weather"))))
                .andExpect(jsonPath("$.usages.length()").value(2));
    }
}
