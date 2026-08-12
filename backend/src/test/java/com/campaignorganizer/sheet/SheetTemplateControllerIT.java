package com.campaignorganizer.sheet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SheetTemplateControllerIT extends AbstractIntegrationTest {

    private static final String DND = """
            {"name":"D&D 5e","system":"dnd5e","sections":[
              {"title":"Core","fields":[
                {"key":"class","label":"Class","type":"TEXT"},
                {"key":"level","label":"Level","type":"NUMBER"},
                {"key":"alignment","label":"Alignment","type":"SELECT","options":["LG","NG","CG"]}
              ]}
            ]}""";

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/sheet-templates", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roundTripsJsonbSchema() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/sheet-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DND))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("D&D 5e"))
                .andExpect(jsonPath("$.sections[0].title").value("Core"))
                .andExpect(jsonPath("$.sections[0].fields[1].key").value("level"))
                .andExpect(jsonPath("$.sections[0].fields[1].type").value("NUMBER"))
                .andExpect(jsonPath("$.sections[0].fields[2].options[0]").value("LG"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Read back from the database (JSONB survives a round trip).
        mockMvc.perform(get("/api/worlds/{w}/sheet-templates/{t}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].fields[0].label").value("Class"))
                .andExpect(jsonPath("$.sections[0].fields[2].options.length()").value(3));

        mockMvc.perform(delete("/api/worlds/{w}/sheet-templates/{t}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsTemplateWithoutName() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/sheet-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"system\":\"x\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
