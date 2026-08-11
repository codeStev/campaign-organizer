package com.campaignorganizer.wiki;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ArticleTemplateControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/article-templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTemplatesWithSections() throws Exception {
        mockMvc.perform(get("/api/article-templates").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[?(@.template == 'CHARACTER')].label").value(org.hamcrest.Matchers.hasItem("Character")))
                .andExpect(jsonPath("$[?(@.template == 'CHARACTER')].sections[0].heading")
                        .value(org.hamcrest.Matchers.hasItem("Appearance")));
    }
}
