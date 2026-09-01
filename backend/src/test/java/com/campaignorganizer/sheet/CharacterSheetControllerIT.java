package com.campaignorganizer.sheet;

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

class CharacterSheetControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String templateId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        templateId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D&D 5e\",\"kind\":\"CHARACTER\",\"sections\":["
                                + "{\"title\":\"Core\",\"fields\":[{\"key\":\"level\",\"label\":\"Level\",\"type\":\"NUMBER\"}]}]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createCampaign(String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/character-sheets", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsWithValuesAndRoundTrips() throws Exception {
        setup();
        String created = mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia\",\"worldTemplateId\":\"" + templateId + "\","
                                + "\"values\":{\"level\":5,\"class\":\"Wizard\",\"alive\":true}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Thalia"))
                .andExpect(jsonPath("$.values.level").value(5))
                .andExpect(jsonPath("$.values.class").value("Wizard"))
                .andExpect(jsonPath("$.values.alive").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/character-sheets/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.level").value(5));

        mockMvc.perform(put("/api/worlds/{w}/character-sheets/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia the Wise\",\"worldTemplateId\":\"" + templateId + "\","
                                + "\"values\":{\"level\":6}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thalia the Wise"))
                .andExpect(jsonPath("$.values.level").value(6));

        mockMvc.perform(delete("/api/worlds/{w}/character-sheets/{s}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void linksToArticle() throws Exception {
        setup();
        String articleId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thalia\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia\",\"worldTemplateId\":\"" + templateId + "\","
                                + "\"articleId\":\"" + articleId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleId").value(articleId));
    }

    @Test
    void rejectsUnknownTemplate() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"worldTemplateId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingTemplateCascadesToSheets() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Doomed\",\"worldTemplateId\":\"" + templateId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/worlds/{w}/field-templates/{t}", worldId, templateId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void assignsAndFiltersByCampaign() throws Exception {
        setup();
        String campA = createCampaign("Ashes");
        String campB = createCampaign("Reckoning");

        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia\",\"worldTemplateId\":\"" + templateId + "\",\"campaignId\":\""
                                + campA + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(campA));
        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Runa\",\"worldTemplateId\":\"" + templateId + "\",\"campaignId\":\""
                                + campB + "\"}"))
                .andExpect(status().isCreated());

        // Filter to campaign A -> only Thalia.
        mockMvc.perform(get("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("campaignId", campA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Thalia"));
        // No filter -> both.
        mockMvc.perform(get("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsForeignCampaign() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"worldTemplateId\":\"" + templateId + "\",\"campaignId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingCampaignUnlinksSheetButKeepsIt() throws Exception {
        setup();
        String camp = createCampaign("Doomed Run");
        String sheetId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Survivor\",\"worldTemplateId\":\"" + templateId + "\",\"campaignId\":\""
                                + camp + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}", worldId, camp)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Sheet survives with campaignId cleared (ON DELETE SET NULL).
        mockMvc.perform(get("/api/worlds/{w}/character-sheets/{s}", worldId, sheetId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Survivor"))
                .andExpect(jsonPath("$.campaignId").doesNotExist());
    }
}
