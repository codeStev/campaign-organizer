package com.campaignorganizer.sheet;

import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/character-sheets/{sheetId}/pdf")
public class CharacterSheetPdfController {

    private final CharacterSheetRepository sheets;
    private final SheetTemplateRepository templates;
    private final CharacterSheetPdfService pdfService;

    public CharacterSheetPdfController(CharacterSheetRepository sheets, SheetTemplateRepository templates,
                                       CharacterSheetPdfService pdfService) {
        this.sheets = sheets;
        this.templates = templates;
        this.pdfService = pdfService;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        CharacterSheet sheet = sheets.findByIdAndWorldId(sheetId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character sheet not found"));
        SheetTemplate template = templates.findByIdAndWorldId(sheet.getTemplateId(), worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        String system = template.getSystem();
        if (system == null || !pdfService.supports(system)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No PDF export available for this sheet's system");
        }

        byte[] pdf = pdfService.fill(system, sheet.getName(), sheet.getValues());
        String filename = "character-" + slug(sheet.getName()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static String slug(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        return s.isEmpty() ? "sheet" : s;
    }
}
