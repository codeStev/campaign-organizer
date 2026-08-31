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

class TagControllerIT extends AbstractIntegrationTest {

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
        mockMvc.perform(get("/api/worlds/{w}/tags", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setsAndFoldsTagsOnAnArticle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String articleId = createArticle(auth, worldId, "Old Man Harrow");

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/tags", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(0));

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/tags", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"  Villain \",\"Recurring\",\"villain\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0]").value("recurring"))
                .andExpect(jsonPath("$.tags[1]").value("villain"));

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/tags", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void unknownArticleReturns404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/tags", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void worldTagListIsDistinctAcrossArticlesAndStatblocks() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String articleId = createArticle(auth, worldId, "Harbor Ghost");
        String statblockId = createStatblock(auth, worldId, "Goblin");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/tags", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"npc\",\"session-1\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/worlds/{w}/statblocks/{s}/tags", worldId, statblockId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"npc\",\"combat\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/worlds/{w}/tags", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("combat"))
                .andExpect(jsonPath("$[1]").value("npc"))
                .andExpect(jsonPath("$[2]").value("session-1"));
    }

    @Test
    void filtersArticlesAndStatblocksByTag() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String taggedArticle = createArticle(auth, worldId, "Harbor Ghost");
        createArticle(auth, worldId, "Old Man Harrow");
        String taggedStatblock = createStatblock(auth, worldId, "Goblin");
        createStatblock(auth, worldId, "Rat");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/tags", worldId, taggedArticle)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"npc\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/worlds/{w}/statblocks/{s}/tags", worldId, taggedStatblock)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"npc\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .param("tag", "npc")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taggedArticle));

        mockMvc.perform(get("/api/worlds/{w}/statblocks", worldId)
                        .param("tag", "npc")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taggedStatblock));

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .param("tag", "nonexistent")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
