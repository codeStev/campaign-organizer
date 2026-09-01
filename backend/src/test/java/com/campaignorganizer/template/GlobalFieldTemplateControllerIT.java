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

/** The world-independent global field template catalog (ADR-0093/ADR-0094). */
class GlobalFieldTemplateControllerIT extends AbstractIntegrationTest {

    private static String dnd(String systemId) {
        return """
            {"name":"D&D 5e","kind":"CHARACTER","systemId":"%s","sections":[
              {"title":"Core","fields":[
                {"key":"level","label":"Level","type":"NUMBER"}
              ]}
            ]}""".formatted(systemId);
    }

    private String createGameSystem(String auth, String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/game-systems")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String auth = authHeader();
        String systemId = createGameSystem(auth, "D&D 5e (crud)");

        String created = mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dnd(systemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("D&D 5e"))
                .andExpect(jsonPath("$.systemId").value(systemId))
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
                        .content("{\"name\":\"D&D 5e (revised)\",\"kind\":\"CHARACTER\",\"systemId\":\"" + systemId
                                + "\",\"sections\":[]}"))
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
        String systemA = createGameSystem(auth, "System A");
        String systemB = createGameSystem(auth, "System B");
        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"kind\":\"CHARACTER\",\"systemId\":\"" + systemA
                                + "\",\"sections\":[]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"kind\":\"STATBLOCK\",\"systemId\":\"" + systemB
                                + "\",\"sections\":[]}"))
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
        String systemId = createGameSystem(auth, "D&D 5e (delete-restrict)");

        String created = mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dnd(systemId)))
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
    void deleteFailsWhileAGlobalStatblockStillReferencesIt() throws Exception {
        String auth = authHeader();
        String systemId = createGameSystem(auth, "D&D 5e (delete-restrict-global-statblock)");

        String created = mockMvc.perform(post("/api/field-templates/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Monster\",\"kind\":\"STATBLOCK\",\"systemId\":\"" + systemId
                                + "\",\"sections\":[]}"))
                .andReturn().getResponse().getContentAsString();
        String globalId = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/statblocks/global")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\",\"systemId\":\"" + systemId + "\",\"globalTemplateId\":\""
                                + globalId + "\"}"))
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
