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
    private final SheetPdfGenerator pdfGenerator;

    public CharacterSheetPdfController(CharacterSheetRepository sheets, SheetTemplateRepository templates,
                                       CharacterSheetPdfService pdfService, SheetPdfGenerator pdfGenerator) {
        this.sheets = sheets;
        this.templates = templates;
        this.pdfService = pdfService;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        CharacterSheet sheet = sheets.findByIdAndWorldId(sheetId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character sheet not found"));
        SheetTemplate template = templates.findByIdAndWorldId(sheet.getTemplateId(), worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        String system = template.getSystem();
        // Use the polished bundled sheet when we have one; otherwise generate a
        // fillable PDF from the template schema (ADR-0029).
        byte[] pdf = (system != null && pdfService.supports(system))
                ? pdfService.fill(system, sheet.getName(), sheet.getValues())
                : pdfGenerator.generate(sheet.getName() + " — " + template.getName(),
                        template.getSections(), sheet.getValues());
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
