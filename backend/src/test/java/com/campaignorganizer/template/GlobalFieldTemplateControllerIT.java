package com.campaignorganizer.template;

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

/** The world-independent global field template catalog (ADR-0093). */
class GlobalFieldTemplateControllerIT extends AbstractIntegrationTest {

    private static final String DND = """
            {"name":"D&D 5e","kind":"CHARACTER","system":"dnd5e","sections":[
              {"title":"Core","fields":[
                {"key":"level","label":"Level","type":"NUMBER"}
              ]}
            ]}""";

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();

        String created = mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DND))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("D&D 5e"))
                .andExpect(jsonPath("$.system").value("dnd5e"))
                .andExpect(jsonPath("$.sections[0].fields[0].key").value("level"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/field-templates/global/{t}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("D&D 5e"));

        mockMvc.perform(put("/api/field-templates/global/{t}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D&D 5e (revised)\",\"kind\":\"CHARACTER\",\"system\":\"dnd5e\","
                                + "\"sections\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("D&D 5e (revised)"));

        mockMvc.perform(delete("/api/field-templates/global/{t}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/field-templates/global/{t}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsTemplateWithoutSystem() throws Exception {
        String auth = authHeader();
        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No system\",\"kind\":\"CHARACTER\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersByKind() throws Exception {
        String auth = authHeader();
        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"kind\":\"CHARACTER\",\"system\":\"a\",\"sections\":[]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"kind\":\"STATBLOCK\",\"system\":\"b\",\"sections\":[]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/field-templates/global?kind=STATBLOCK")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("B")))
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("A"))));
    }

    @Test
    void deleteFailsWhileACharacterSheetStillReferencesIt() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DND))
                .andReturn().getResponse().getContentAsString();
        String globalId = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia\",\"globalTemplateId\":\"" + globalId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/field-templates/global/{t}", globalId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    @Test
    void notFoundForUnknownTemplate() throws Exception {
        String auth = authHeader();
        mockMvc.perform(get("/api/field-templates/global/{t}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
