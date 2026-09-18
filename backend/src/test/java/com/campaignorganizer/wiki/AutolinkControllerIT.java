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

class AutolinkControllerIT extends AbstractIntegrationTest {

    private String createArticle(String auth, String worldId, String title, String body) throws Exception {
        String json = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/articles/autolink-candidates", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scanFindsUnlinkedMentionGroupedBySourceArticle() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String goblinId = createArticle(auth, worldId, "Goblin", "A fearsome creature.");
        String notesId = createArticle(auth, worldId, "Notes", "A goblin was seen nearby.");

        mockMvc.perform(get("/api/worlds/{w}/articles/autolink-candidates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].articleId").value(notesId))
                .andExpect(jsonPath("$[0].matches.length()").value(1))
                .andExpect(jsonPath("$[0].matches[0].targetArticleId").value(goblinId))
                .andExpect(jsonPath("$[0].matches[0].matchedText").value("goblin"))
                .andExpect(jsonPath("$[0].matches[0].candidateNames[0]").value("Goblin"));
    }

    @Test
    void scanOffersAliasesAsCandidateNames() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String targetId = createArticle(auth, worldId, "Robert Gutkind", "A local blacksmith.");
        mockMvc.perform(put("/api/worlds/{w}/articles/{a}/aliases", worldId, targetId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[\"Bob\"]}"))
                .andExpect(status().isOk());
        createArticle(auth, worldId, "Notes", "Talked to Bob today.");

        mockMvc.perform(get("/api/worlds/{w}/articles/autolink-candidates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[0].targetArticleId").value(targetId))
                .andExpect(jsonPath("$[0].matches[0].matchedText").value("Bob"))
                .andExpect(jsonPath("$[0].matches[0].candidateNames[0]").value("Robert Gutkind"))
                .andExpect(jsonPath("$[0].matches[0].candidateNames[1]").value("Bob"));
    }

    @Test
    void applyConvertsOnlySelectedOccurrenceUsingChosenNameAsTargetAndPreservesOriginalWording() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String goblinId = createArticle(auth, worldId, "Goblin", "A fearsome creature.");
        String notesId = createArticle(auth, worldId, "Notes", "A goblin, then another goblin.");

        mockMvc.perform(post("/api/worlds/{w}/articles/{a}/autolink", worldId, notesId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"targetArticleId\":\"" + goblinId
                                + "\",\"occurrenceIndex\":1,\"chosenName\":\"Goblin\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("A goblin, then another [[Goblin|goblin]]."))
                .andExpect(jsonPath("$.bodyHtml").value(org.hamcrest.Matchers.containsString(
                        "data-article-id=\"" + goblinId + "\"")));

        // A revision was recorded, same as any manual edit (ADR-0026).
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}/revisions", worldId, notesId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
