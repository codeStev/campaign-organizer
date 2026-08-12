package com.campaignorganizer.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

class CharacterSheetPdfControllerIT extends AbstractIntegrationTest {

    private String auth;
    private String worldId;

    private String template(String system) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/sheet-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T\",\"system\":\"" + system + "\",\"sections\":[]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String sheet(String templateId, String values) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/character-sheets", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thalia\",\"templateId\":\"" + templateId + "\",\"values\":" + values + "}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/character-sheets/{s}/pdf",
                        java.util.UUID.randomUUID(), java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fillsAndReturnsDnd5ePdf() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String templateId = template("dnd5e");
        String sheetId = sheet(templateId,
                "{\"class\":\"Wizard\",\"level\":5,\"str\":16,\"dex\":12,\"race\":\"Elf\"}");

        MockHttpServletResponse res = mockMvc.perform(get(
                        "/api/worlds/{w}/character-sheets/{s}/pdf", worldId, sheetId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn().getResponse();

        byte[] pdf = res.getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        // Read the generated PDF back and confirm the fields were filled.
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertThat(form.getField("CharacterName").getValueAsString()).isEqualTo("Thalia");
            assertThat(form.getField("STR").getValueAsString()).isEqualTo("16");
            assertThat(form.getField("STRmod").getValueAsString()).isEqualTo("+3"); // (16-10)/2
            assertThat(form.getField("ClassLevel").getValueAsString()).contains("Wizard").contains("5");
            assertThat(form.getField("Race ").getValueAsString()).isEqualTo("Elf");
        }
    }

    @Test
    void generatesPdfFromTemplateForCustomSystem() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        // A homebrew template with a custom field; no bundled system PDF exists.
        String templateId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/sheet-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Homebrew\",\"system\":\"custom\",\"sections\":["
                                + "{\"title\":\"Core\",\"fields\":["
                                + "{\"key\":\"grit\",\"label\":\"Grit\",\"type\":\"NUMBER\"},"
                                + "{\"key\":\"alive\",\"label\":\"Alive\",\"type\":\"BOOLEAN\"}]}]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String sheetId = sheet(templateId, "{\"grit\":7,\"alive\":true}");

        byte[] pdf = mockMvc.perform(get("/api/worlds/{w}/character-sheets/{s}/pdf", worldId, sheetId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertThat(form.getField("grit")).isNotNull();
            assertThat(form.getField("grit").getValueAsString()).isEqualTo("7");
            assertThat(form.getField("alive")).isNotNull();
        }
    }

    @Test
    void rendersCirclesAsCheckboxRowFilledToValue() throws Exception {
        auth = authHeader();
        worldId = createWorld(auth);
        String templateId = JsonPath.read(mockMvc.perform(post("/api/worlds/{w}/sheet-templates", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trackers\",\"system\":\"custom\",\"sections\":["
                                + "{\"title\":\"State\",\"fields\":["
                                + "{\"key\":\"str\",\"label\":\"STR\",\"type\":\"NUMBER\",\"width\":\"HALF\"},"
                                + "{\"key\":\"dex\",\"label\":\"DEX\",\"type\":\"NUMBER\",\"width\":\"HALF\"},"
                                + "{\"key\":\"wounds\",\"label\":\"Wounds\",\"type\":\"CIRCLES\",\"count\":5}]}]}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String sheetId = sheet(templateId, "{\"str\":16,\"dex\":12,\"wounds\":3}");

        byte[] pdf = mockMvc.perform(get("/api/worlds/{w}/character-sheets/{s}/pdf", worldId, sheetId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            // Five circle checkboxes; the first three are checked (wounds = 3).
            assertThat(form.getField("wounds_1").getValueAsString()).isNotEqualTo("Off");
            assertThat(form.getField("wounds_3").getValueAsString()).isNotEqualTo("Off");
            assertThat(form.getField("wounds_4").getValueAsString()).isEqualTo("Off");
            assertThat(form.getField("wounds_5").getValueAsString()).isEqualTo("Off");
            // The two HALF-width ability fields both exist (side by side).
            assertThat(form.getField("str").getValueAsString()).isEqualTo("16");
            assertThat(form.getField("dex").getValueAsString()).isEqualTo("12");
        }
    }
}
