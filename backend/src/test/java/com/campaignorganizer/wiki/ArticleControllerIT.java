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
