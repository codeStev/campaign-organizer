package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.out.pdf.CharacterSheetPdfService;
import com.campaignorganizer.characters.adapter.sheet.out.pdf.SheetPdfGenerator;
import com.campaignorganizer.characters.application.sheet.port.in.GetCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.template.port.in.GetFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import java.util.List;
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
    private final GetFieldTemplateUseCase getTemplateUseCase;
    private final GetGlobalFieldTemplateUseCase getGlobalTemplateUseCase;
    private final CharacterSheetPdfService pdfService;
    private final SheetPdfGenerator pdfGenerator;

    public CharacterSheetPdfController(GetCharacterSheetUseCase getSheetUseCase,
                                       GetFieldTemplateUseCase getTemplateUseCase,
                                       GetGlobalFieldTemplateUseCase getGlobalTemplateUseCase,
                                       CharacterSheetPdfService pdfService, SheetPdfGenerator pdfGenerator) {
        this.getSheetUseCase = getSheetUseCase;
        this.getTemplateUseCase = getTemplateUseCase;
        this.getGlobalTemplateUseCase = getGlobalTemplateUseCase;
        this.pdfService = pdfService;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        CharacterSheetView sheet = getSheetUseCase.get(worldId, sheetId);

        String system;
        String templateName;
        List<TemplateSection> sections;
        if (sheet.worldTemplateId() != null) {
            FieldTemplateView template = getTemplateUseCase.get(worldId, sheet.worldTemplateId());
            system = template.system();
            templateName = template.name();
            sections = template.sections();
        } else {
            GlobalFieldTemplateView template = getGlobalTemplateUseCase.get(sheet.globalTemplateId());
            system = template.system();
            templateName = template.name();
            sections = template.sections();
        }

        // Use the polished bundled sheet when we have one; otherwise generate a
        // fillable PDF from the template schema (ADR-0029).
        byte[] pdf = (system != null && pdfService.supports(system))
                ? pdfService.fill(system, sheet.name(), sheet.values())
                : pdfGenerator.generate(sheet.name() + " — " + templateName, sections, sheet.values());
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
