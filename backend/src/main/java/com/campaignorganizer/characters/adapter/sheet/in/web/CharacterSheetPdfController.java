package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.out.pdf.CharacterSheetPdfService;
import com.campaignorganizer.characters.adapter.sheet.out.pdf.SheetPdfGenerator;
import com.campaignorganizer.characters.application.sheet.port.in.GetCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.GetSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds/{worldId}/character-sheets/{sheetId}/pdf")
public class CharacterSheetPdfController {

    private final GetCharacterSheetUseCase getSheetUseCase;
    private final GetSheetTemplateUseCase getTemplateUseCase;
    private final CharacterSheetPdfService pdfService;
    private final SheetPdfGenerator pdfGenerator;

    public CharacterSheetPdfController(GetCharacterSheetUseCase getSheetUseCase,
                                       GetSheetTemplateUseCase getTemplateUseCase,
                                       CharacterSheetPdfService pdfService, SheetPdfGenerator pdfGenerator) {
        this.getSheetUseCase = getSheetUseCase;
        this.getTemplateUseCase = getTemplateUseCase;
        this.pdfService = pdfService;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        CharacterSheetView sheet = getSheetUseCase.get(worldId, sheetId);
        SheetTemplateView template = getTemplateUseCase.get(worldId, sheet.templateId());

        String system = template.system();
        // Use the polished bundled sheet when we have one; otherwise generate a
        // fillable PDF from the template schema (ADR-0029).
        byte[] pdf = (system != null && pdfService.supports(system))
                ? pdfService.fill(system, sheet.name(), sheet.values())
                : pdfGenerator.generate(sheet.name() + " — " + template.name(),
                        template.sections(), sheet.values());
        String filename = "character-" + slug(sheet.name()) + ".pdf";
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
