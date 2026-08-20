package com.campaignorganizer.template;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class BuiltinFieldTemplateControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/field-templates/builtin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsStarterTemplatesWithSchema() throws Exception {
        mockMvc.perform(get("/api/field-templates/builtin").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.system == 'dnd5e')].name").value(Matchers.hasItem("D&D 5e")))
                .andExpect(jsonPath("$[?(@.system == 'dnd5e')].sections[1].fields[0].key")
                        .value(Matchers.hasItem("str")));
    }

    @Test
    void filtersByKind() throws Exception {
        mockMvc.perform(get("/api/field-templates/builtin?kind=CHARACTER")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(Matchers.hasItem("D&D 5e")))
                .andExpect(jsonPath("$[*].name").value(Matchers.not(Matchers.hasItem("D&D 5e Monster"))));

        mockMvc.perform(get("/api/field-templates/builtin?kind=STATBLOCK")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(Matchers.hasItem("D&D 5e Monster")))
                .andExpect(jsonPath("$[*].name").value(Matchers.not(Matchers.hasItem("D&D 5e"))));
    }

    @Test
    void monsterStarterHasLegendaryResistanceCircles() throws Exception {
        mockMvc.perform(get("/api/field-templates/builtin").header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'D&D 5e Monster')].sections[3].fields[2].type")
                        .value(Matchers.hasItem("CIRCLES")))
                .andExpect(jsonPath("$[?(@.name == 'D&D 5e Monster')].sections[3].fields[2].count")
                        .value(Matchers.hasItem(3)));
    }
}
