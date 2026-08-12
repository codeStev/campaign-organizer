package com.campaignorganizer.sheet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class BuiltinSheetTemplateControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/sheet-templates/builtin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsStarterTemplatesWithSchema() throws Exception {
        mockMvc.perform(get("/api/sheet-templates/builtin").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.system == 'dnd5e')].name").value(Matchers.hasItem("D&D 5e")))
                .andExpect(jsonPath("$[?(@.system == 'dnd5e')].sections[1].fields[0].key")
                        .value(Matchers.hasItem("str")));
    }
}
