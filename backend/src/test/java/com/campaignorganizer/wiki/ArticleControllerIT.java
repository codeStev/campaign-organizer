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
                                + "\"body\":\"A bustling market.\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.slug").value("the-grand-bazaar"))
                .andExpect(jsonPath("$.template").value("LOCATION"))
                .andExpect(jsonPath("$.body").value("A bustling market."))
                .andExpect(jsonPath("$.bodyHtml").value(org.hamcrest.Matchers.containsString(
                        "<p>A bustling market.</p>")))
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
                "{\"title\":\"Eldergrove\",\"body\":\"An ancient whispering forest.\"}");
        createArticle(auth, worldId, "{\"title\":\"Ironhold\",\"body\":\"A mountain fortress.\"}");

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
                        .content("{\"title\":\"Final\",\"body\":\"Done.\"}"))
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
                        .content("{\"body\":\"no title\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ranksTitleMatchesAboveBodyMatches() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        // Body-only match created first (older), title match created second.
        createArticle(auth, worldId,
                "{\"title\":\"Ironhold\",\"body\":\"A deep forest lies to the north.\"}");
        createArticle(auth, worldId, "{\"title\":\"Forest Kingdom\"}");

        mockMvc.perform(get("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("q", "forest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Forest Kingdom"));
    }

    @Test
    void sanitizesScriptFromBodyOnRender() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = createArticle(auth, worldId,
                "{\"title\":\"XSS\",\"body\":\"ok\\n\\n<script>alert('x')</script>\"}");
        String rawBody = JsonPath.read(created, "$.body");
        String bodyHtml = JsonPath.read(created, "$.bodyHtml");

        // Write-time sanitization is gone (ADR-0054) — the raw Markdown is stored as submitted.
        org.assertj.core.api.Assertions.assertThat(rawBody).containsIgnoringCase("script");
        // The rendered HTML — the only thing ever shown to a browser — is still sanitized.
        org.assertj.core.api.Assertions.assertThat(bodyHtml).doesNotContainIgnoringCase("script");
    }

    @Test
    void resolvesWikiLinksInBodyHtml() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String goblinId = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"Goblin\"}"), "$.id");

        String body = "{\"title\":\"Bestiary\",\"body\":"
                + "\"See [[Goblin]], [[goblin|the goblins]] and [[Missing]].\"}";
        String created = createArticle(auth, worldId, body);

        String bodyHtml = JsonPath.read(created, "$.bodyHtml");
        String rawBody = JsonPath.read(created, "$.body");

        org.assertj.core.api.Assertions.assertThat(bodyHtml)
                .contains("class=\"wiki-link\"")
                .contains("data-article-id=\"" + goblinId + "\"")
                .contains(">Goblin</a>")
                .contains(">the goblins</a>")
                .contains("class=\"broken-link\"")
                .contains(">Missing</span>");
        // Raw body keeps the [[...]] tokens for editing.
        org.assertj.core.api.Assertions.assertThat(rawBody).contains("[[Goblin]]");
    }

    @Test
    void imageWithWidthRendersRawHtmlThroughMarkdown() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = createArticle(auth, worldId,
                "{\"title\":\"Portrait\",\"body\":"
                        + "\"before\\n\\n<img src=\\\"/api/media/abc/content\\\" width=\\\"480\\\">\\n\\nafter\"}");
        String bodyHtml = JsonPath.read(created, "$.bodyHtml");

        org.assertj.core.api.Assertions.assertThat(bodyHtml)
                .contains("src=\"/api/media/abc/content\"")
                .contains("width=\"480\"");
    }

    @Test
    void standardMarkdownFormattingRenders() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = createArticle(auth, worldId,
                "{\"title\":\"Formatting\",\"body\":"
                        + "\"# Heading\\n\\n**bold** and _italic_ and a list:\\n\\n- one\\n- two\"}");
        String bodyHtml = JsonPath.read(created, "$.bodyHtml");

        org.assertj.core.api.Assertions.assertThat(bodyHtml)
                .contains("<h1>Heading</h1>")
                .contains("<strong>bold</strong>")
                .contains("<em>italic</em>")
                .contains("<li>one</li>")
                .contains("<li>two</li>");
    }

    @Test
    void createsAndReadsAParentChildArticlePair() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String parentId = JsonPath.read(
                createArticle(auth, worldId, "{\"title\":\"Sunken Temple\",\"template\":\"LOCATION\"}"),
                "$.id");

        String created = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Entry Hall\",\"template\":\"LOCATION\","
                                + "\"parentArticleId\":\"" + parentId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentArticleId").value(parentId))
                .andReturn().getResponse().getContentAsString();
        String childId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}", worldId, childId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentArticleId").value(parentId));
    }

    @Test
    void rejectsSelfAndForeignParent() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String id = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"Solo\"}"), "$.id");

        mockMvc.perform(put("/api/worlds/{w}/articles/{a}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Solo\",\"parentArticleId\":\"" + id + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Orphan\",\"parentArticleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAMultiHopCycle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String aId = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"A\"}"), "$.id");
        String bId = JsonPath.read(
                createArticle(auth, worldId, "{\"title\":\"B\",\"parentArticleId\":\"" + aId + "\"}"), "$.id");

        // Attempt to set A's parent to B, its own grandchild-to-be (B's parent is A).
        mockMvc.perform(put("/api/worlds/{w}/articles/{a}", worldId, aId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"parentArticleId\":\"" + bId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingAParentLeavesChildrenAsTopLevel() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String parentId = JsonPath.read(createArticle(auth, worldId, "{\"title\":\"Parent\"}"), "$.id");
        String childId = JsonPath.read(
                createArticle(auth, worldId, "{\"title\":\"Child\",\"parentArticleId\":\"" + parentId + "\"}"),
                "$.id");

        mockMvc.perform(delete("/api/worlds/{w}/articles/{a}", worldId, parentId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}", worldId, childId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentArticleId").doesNotExist());
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
