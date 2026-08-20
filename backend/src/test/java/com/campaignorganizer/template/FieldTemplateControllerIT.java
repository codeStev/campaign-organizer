package com.campaignorganizer.template;

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

class FieldTemplateControllerIT extends AbstractIntegrationTest {

    private static final String DND = """
            {"name":"D&D 5e","kind":"CHARACTER","system":"dnd5e","sections":[
              {"title":"Core","fields":[
                {"key":"class","label":"Class","type":"TEXT"},
                {"key":"level","label":"Level","type":"NUMBER"},
                {"key":"alignment","label":"Alignment","type":"SELECT","options":["LG","NG","CG"]}
              ]}
            ]}""";

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/field-templates", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roundTripsJsonbSchema() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
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
        mockMvc.perform(get("/api/worlds/{w}/field-templates/{t}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].fields[0].label").value("Class"))
                .andExpect(jsonPath("$.sections[0].fields[2].options.length()").value(3));

        mockMvc.perform(delete("/api/worlds/{w}/field-templates/{t}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void roundTripsWidthAndCirclesFields() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String created = mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Homebrew\",\"kind\":\"CHARACTER\",\"sections\":[{\"title\":\"Core\",\"fields\":["
                                + "{\"key\":\"str\",\"label\":\"STR\",\"type\":\"NUMBER\",\"width\":\"HALF\"},"
                                + "{\"key\":\"dex\",\"label\":\"DEX\",\"type\":\"NUMBER\",\"width\":\"HALF\"},"
                                + "{\"key\":\"wounds\",\"label\":\"Wounds\",\"type\":\"CIRCLES\",\"count\":5}]}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sections[0].fields[0].width").value("HALF"))
                .andExpect(jsonPath("$.sections[0].fields[2].type").value("CIRCLES"))
                .andExpect(jsonPath("$.sections[0].fields[2].count").value(5))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Read back from the database.
        mockMvc.perform(get("/api/worlds/{w}/field-templates/{t}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].fields[1].width").value("HALF"))
                .andExpect(jsonPath("$.sections[0].fields[2].count").value(5));
    }

    @Test
    void acceptsLegacyFieldsWithoutWidthOrCount() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        // Fields without the new width/count properties must still deserialize.
        mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legacy\",\"kind\":\"CHARACTER\",\"sections\":[{\"title\":\"S\",\"fields\":["
                                + "{\"key\":\"a\",\"label\":\"A\",\"type\":\"TEXT\"}]}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sections[0].fields[0].key").value("a"));
    }

    @Test
    void rejectsTemplateWithoutName() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"CHARACTER\",\"system\":\"x\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTemplateWithoutKind() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No kind\",\"sections\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersByKind() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A Character Template\",\"kind\":\"CHARACTER\",\"sections\":[]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A Statblock Template\",\"kind\":\"STATBLOCK\",\"sections\":[]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/field-templates?kind=STATBLOCK", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("STATBLOCK"));
    }
}
