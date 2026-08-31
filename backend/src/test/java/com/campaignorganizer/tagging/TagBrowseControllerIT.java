package com.campaignorganizer.tagging;

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

/** Cross-entity browse-by-tag (ADR-0083), composed via interchange.tags. */
class TagBrowseControllerIT extends AbstractIntegrationTest {

    private String createArticle(String auth, String worldId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createStatblock(String auth, String worldId, String name) throws Exception {
        String body = mockMvc.perform(post("/api/worlds/{w}/statblocks", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"stats\":{}}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/tags/{t}/entities", UUID.randomUUID(), "npc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownWorldReturns404() throws Exception {
        String auth = authHeader();
        mockMvc.perform(get("/api/worlds/{w}/tags/{t}/entities", UUID.randomUUID(), "npc")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void browseReturnsArticlesAndStatblocksTogether() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String articleId = createArticle(auth, worldId, "Harbor Ghost");
        String statblockId = createStatblock(auth, worldId, "Goblin");
        createArticle(auth, worldId, "Untagged article");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/tags", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"npc\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/worlds/{w}/statblocks/{s}/tags", worldId, statblockId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"  NPC  \"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/worlds/{w}/tags/{t}/entities", worldId, "npc")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("npc"))
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].id").value(articleId))
                .andExpect(jsonPath("$.statblocks.length()").value(1))
                .andExpect(jsonPath("$.statblocks[0].id").value(statblockId));
    }

    @Test
    void browseWithNoMatchesReturnsEmptyLists() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/tags/{t}/entities", worldId, "nonexistent")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles.length()").value(0))
                .andExpect(jsonPath("$.statblocks.length()").value(0));
    }
}
