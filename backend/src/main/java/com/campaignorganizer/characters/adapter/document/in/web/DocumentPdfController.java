package com.campaignorganizer.characters.adapter.document.in.web;

import com.campaignorganizer.characters.adapter.sheet.out.pdf.SheetPdfGenerator;
import com.campaignorganizer.characters.application.document.port.in.GetDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.application.template.port.in.GetFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Documents have no per-system bundled hand-designed PDF (unlike character
 * sheets' D&amp;D 5e one, ADR-0028) — always the generic AcroForm generator.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/documents/{documentId}/pdf")
public class DocumentPdfController {

    private final GetDocumentUseCase getDocumentUseCase;
    private final GetFieldTemplateUseCase getTemplateUseCase;
    private final SheetPdfGenerator pdfGenerator;

    public DocumentPdfController(GetDocumentUseCase getDocumentUseCase,
                                 GetFieldTemplateUseCase getTemplateUseCase,
                                 SheetPdfGenerator pdfGenerator) {
        this.getDocumentUseCase = getDocumentUseCase;
        this.getTemplateUseCase = getTemplateUseCase;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@PathVariable UUID worldId, @PathVariable UUID documentId) {
        DocumentView document = getDocumentUseCase.get(worldId, documentId);
        FieldTemplateView template = getTemplateUseCase.get(worldId, document.templateId());

        byte[] pdf = pdfGenerator.generate(document.name() + " — " + template.name(),
                template.sections(), document.values());
        String filename = "document-" + slug(document.name()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static String slug(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        return s.isEmpty() ? "document" : s;
    }
}
