package com.campaignorganizer.campaign;

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
        return JsonPath.read(mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString(), "$.id");
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
}
