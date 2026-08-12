package com.campaignorganizer.relationship;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class RelationshipControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
    }

    private String article(String title) throws Exception {
        String body = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/relationships", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListUpdateDelete() throws Exception {
        setup();
        String alice = article("Alice");
        String bob = article("Bob");

        String created = mockMvc.perform(post("/api/worlds/{w}/relationships", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\"" + bob
                                + "\",\"label\":\"parent of\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("parent of"))
                .andExpect(jsonPath("$.directed").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/relationships", worldId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/worlds/{w}/relationships/{r}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\"" + bob
                                + "\",\"label\":\"allied with\",\"directed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("allied with"))
                .andExpect(jsonPath("$.directed").value(false));

        mockMvc.perform(delete("/api/worlds/{w}/relationships/{r}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsSelfRelationship() throws Exception {
        setup();
        String alice = article("Alice");
        mockMvc.perform(post("/api/worlds/{w}/relationships", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\"" + alice + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownEndpoint() throws Exception {
        setup();
        String alice = article("Alice");
        mockMvc.perform(post("/api/worlds/{w}/relationships", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsNeighbourhoodAndCascadesOnArticleDelete() throws Exception {
        setup();
        String alice = article("Alice");
        String bob = article("Bob");
        String carol = article("Carol");

        // Alice-Bob and Alice-Carol; Bob's neighbourhood is just one edge.
        mockMvc.perform(post("/api/worlds/{w}/relationships", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\"" + bob + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/worlds/{w}/relationships", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromArticleId\":\"" + alice + "\",\"toArticleId\":\"" + carol + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/relationships", worldId, bob)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Deleting Alice removes both of her edges (ON DELETE CASCADE).
        mockMvc.perform(delete("/api/worlds/{w}/articles/{a}", worldId, alice)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/relationships", worldId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
