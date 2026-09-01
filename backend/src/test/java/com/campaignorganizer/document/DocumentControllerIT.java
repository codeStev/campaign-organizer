package com.campaignorganizer.document;

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

class DocumentControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;
    private String templateId;

    private void setup() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        templateId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/field-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Session Zero\",\"kind\":\"DOCUMENT\",\"sections\":["
                                + "{\"title\":\"Tone\",\"fields\":[{\"key\":\"lines\",\"label\":\"Lines\","
                                + "\"type\":\"TEXTAREA\"}]}]}"))
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
        mockMvc.perform(get("/api/worlds/{w}/documents", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsWithValuesAndRoundTrips() throws Exception {
        setup();
        String created = mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Session Zero — Ashes\",\"templateId\":\"" + templateId + "\","
                                + "\"values\":{\"lines\":\"No on-screen animal harm\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Session Zero — Ashes"))
                .andExpect(jsonPath("$.values.lines").value("No on-screen animal harm"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/documents/{d}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.lines").value("No on-screen animal harm"));

        mockMvc.perform(put("/api/worlds/{w}/documents/{d}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Session Zero — Ashes (revised)\",\"templateId\":\""
                                + templateId + "\",\"values\":{\"lines\":\"Updated\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Session Zero — Ashes (revised)"))
                .andExpect(jsonPath("$.values.lines").value("Updated"));

        mockMvc.perform(delete("/api/worlds/{w}/documents/{d}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsUnknownTemplate() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"templateId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonDocumentTemplate() throws Exception {
        setup();
        String characterTemplateId = JsonPath.read(mockMvc.perform(
                        post("/api/worlds/{w}/field-templates", worldId)
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"PC\",\"kind\":\"CHARACTER\",\"sections\":[]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"templateId\":\"" + characterTemplateId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingTemplateCascadesToDocuments() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Doomed\",\"templateId\":\"" + templateId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/worlds/{w}/field-templates/{t}", worldId, templateId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void assignsAndFiltersByCampaign() throws Exception {
        setup();
        String campA = createCampaign("Ashes");
        String campB = createCampaign("Reckoning");

        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ashes Zero\",\"templateId\":\"" + templateId
                                + "\",\"campaignId\":\"" + campA + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(campA));
        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reckoning Zero\",\"templateId\":\"" + templateId
                                + "\",\"campaignId\":\"" + campB + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("campaignId", campA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ashes Zero"));
        mockMvc.perform(get("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsForeignCampaign() throws Exception {
        setup();
        mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"templateId\":\"" + templateId + "\",\"campaignId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingCampaignUnlinksDocumentButKeepsIt() throws Exception {
        setup();
        String camp = createCampaign("Doomed Run");
        String documentId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Survivor\",\"templateId\":\"" + templateId
                                + "\",\"campaignId\":\"" + camp + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/worlds/{w}/campaigns/{c}", worldId, camp)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worlds/{w}/documents/{d}", worldId, documentId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Survivor"))
                .andExpect(jsonPath("$.campaignId").doesNotExist());
    }

    @Test
    void exportsPdf() throws Exception {
        setup();
        String documentId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/documents", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Printable\",\"templateId\":\"" + templateId + "\","
                                + "\"values\":{\"lines\":\"Fade to black on violence\"}}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/worlds/{w}/documents/{d}/pdf", worldId, documentId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    org.assertj.core.api.Assertions.assertThat(body.length).isGreaterThan(0);
                    org.assertj.core.api.Assertions.assertThat(new String(body, 0, 4,
                            java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
                });
    }
}
