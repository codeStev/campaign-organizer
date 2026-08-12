package com.campaignorganizer.wiki;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ArticleRevisionControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String articleId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        articleId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft\",\"body\":\"<p>v1</p>\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void editBody(String body) throws Exception {
        mockMvc.perform(put("/api/worlds/{w}/articles/{a}", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft\",\"body\":\"" + body + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void newArticleHasNoRevisions() throws Exception {
        setup();
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/revisions", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eachUpdateSnapshotsPriorState() throws Exception {
        setup();
        editBody("<p>v2</p>");
        editBody("<p>v3</p>");

        // Two edits => two snapshots (v1 and v2), newest first.
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/revisions", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("<p>v2</p>"))
                .andExpect(jsonPath("$[1].body").value("<p>v1</p>"));
    }

    @Test
    void restoresAPriorRevisionAndKeepsItUndoable() throws Exception {
        setup();
        editBody("<p>v2</p>"); // snapshots v1
        String revs = mockMvc.perform(get("/api/worlds/{w}/articles/{a}/revisions", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString();
        String v1Id = JsonPath.read(revs, "$[0].id");

        // Restore v1; the current (v2) state is snapshotted first.
        mockMvc.perform(post("/api/worlds/{w}/articles/{a}/revisions/{r}/restore", worldId, articleId, v1Id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("<p>v1</p>"));

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/revisions", worldId, articleId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("<p>v2</p>")); // the snapshot from the restore
    }
}
