package com.campaignorganizer.wiki;

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

class ArticleAliasControllerIT extends AbstractIntegrationTest {

    private String createArticle(String auth, String worldId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/aliases", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownArticleReturns404() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/aliases", worldId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void setsFoldsAndPreservesCasingOfAliases() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String articleId = createArticle(auth, worldId, "Robert Gutkind");

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/aliases", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliases.length()").value(0));

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"  Bob \",\"Jasper\",\"bob\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliases.length()").value(2))
                .andExpect(jsonPath("$.aliases[0]").value("Bob"))
                .andExpect(jsonPath("$.aliases[1]").value("Jasper"));
    }

    /** Same regression shape as the tags fix (TagControllerIT): re-saving a kept alias
     * must not violate the (article_id, alias) primary key. */
    @Test
    void savingAliasesASecondTimeKeepingAnExistingOneSucceeds() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String articleId = createArticle(auth, worldId, "Robert Gutkind");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"Bob\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"Bob\",\"Jasper\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliases.length()").value(2));
    }

    @Test
    void aliasResolvesToArticleInAnotherArticlesBody() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String targetId = createArticle(auth, worldId, "Robert Gutkind");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, targetId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"Bob\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Notes\",\"body\":\"Talked to [[Bob]] today.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bodyHtml").value(org.hamcrest.Matchers.containsString(
                        "data-article-id=\"" + targetId + "\"")));
    }
}
