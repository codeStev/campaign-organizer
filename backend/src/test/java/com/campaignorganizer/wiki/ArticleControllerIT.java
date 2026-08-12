package com.campaignorganizer.wiki;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ArticleControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/articles", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownWorldReturns404() throws Exception {
        String auth = authHeader();
        mockMvc.perform(get("/api/worlds/{w}/articles", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsAndReadsArticleWithGeneratedSlug() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Grand Bazaar\",\"template\":\"LOCATION\","
                                + "\"body\":\"<p>A bustling market.</p>\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.slug").value("the-grand-bazaar"))
                .andExpect(jsonPath("$.template").value("LOCATION"))
                .andExpect(jsonPath("$.body").value("<p>A bustling market.</p>"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Grand Bazaar"));
    }

    @Test
    void deduplicatesSlugWithinWorld() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        createArticle(auth, worldId, "{\"title\":\"Goblin\"}");
        String second = createArticle(auth, worldId, "{\"title\":\"Goblin\"}");

        assertSlug(second, "goblin-2");
    }

    @Test
    void searchFindsArticleByBody() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        createArticle(auth, worldId,
                "{\"title\":\"Eldergrove\",\"body\":\"<p>An ancient whispering forest.</p>\"}");
        createArticle(auth, worldId, "{\"title\":\"Ironhold\",\"body\":\"<p>A mountain fortress.</p>\"}");

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("q", "whispering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Eldergrove"));
    }

    @Test
    void fuzzySearchMatchesPartialWordsAndTypos() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        createArticle(auth, worldId, "{\"title\":\"Waterdeep\"}");
        createArticle(auth, worldId, "{\"title\":\"Ironhold\"}");

        // Partial word (the old full-text search returned nothing here).
        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("q", "water"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Waterdeep"));

        // Typo tolerance via trigram similarity.
        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("q", "watredeep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Waterdeep"));
    }

    @Test
    void updateAndDelete() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String id = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"Draft\"}"), "$.id");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Final\",\"body\":\"<p>Done.</p>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.slug").value("draft")); // slug stays stable

        mockMvc.perform(delete("/api/worlds/{w}/articles/{a}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsArticleWithoutTitle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"<p>no title</p>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ranksTitleMatchesAboveBodyMatches() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        // Body-only match created first (older), title match created second.
        createArticle(auth, worldId,
                "{\"title\":\"Ironhold\",\"body\":\"<p>A deep forest lies to the north.</p>\"}");
        createArticle(auth, worldId, "{\"title\":\"Forest Kingdom\"}");

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("q", "forest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Forest Kingdom"));
    }

    @Test
    void sanitizesScriptFromBodyOnWrite() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = createArticle(auth, worldId,
                "{\"title\":\"XSS\",\"body\":\"<p>ok</p><script>alert('x')</script>\"}");
        String rawBody = JsonPath.read(created, "$.body");
        String bodyHtml = JsonPath.read(created, "$.bodyHtml");

        org.assertj.core.api.Assertions.assertThat(rawBody).contains("<p>ok</p>");
        org.assertj.core.api.Assertions.assertThat(rawBody).doesNotContainIgnoringCase("script");
        org.assertj.core.api.Assertions.assertThat(bodyHtml).doesNotContainIgnoringCase("script");
    }

    @Test
    void resolvesWikiLinksInBodyHtml() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String goblinId = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"Goblin\"}"), "$.id");

        String body = "{\"title\":\"Bestiary\",\"body\":"
                + "\"<p>See [[Goblin]], [[goblin|the goblins]] and [[Missing]].</p>\"}";
        String created = createArticle(auth, worldId, body);

        String bodyHtml = JsonPath.read(created, "$.bodyHtml");
        String rawBody = JsonPath.read(created, "$.body");

        org.assertj.core.api.Assertions.assertThat(bodyHtml)
                .contains("<a class=\"wiki-link\" data-article-id=\"" + goblinId + "\" href=\"#\">Goblin</a>")
                .contains(">the goblins</a>")
                .contains("<span class=\"broken-link\">Missing</span>");
        // Raw body keeps the [[...]] tokens for editing.
        org.assertj.core.api.Assertions.assertThat(rawBody).contains("[[Goblin]]");
    }

    private String createArticle(String auth, String worldId, String json) throws Exception {
        return mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void assertSlug(String articleJson, String expected) {
        String slug = JsonPath.read(articleJson, "$.slug");
        org.assertj.core.api.Assertions.assertThat(slug).isEqualTo(expected);
    }
}
