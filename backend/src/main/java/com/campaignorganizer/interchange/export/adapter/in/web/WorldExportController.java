package com.campaignorganizer.interchange.export.adapter.in.web;

import com.campaignorganizer.interchange.export.application.port.in.ExportWorldUseCase;
import com.campaignorganizer.interchange.export.application.port.in.WorldExportBundle;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the world export bundle as a downloadable JSON file (FR-22). */
@RestController
@RequestMapping("/api/worlds/{worldId}/export")
public class WorldExportController {

    private final ExportWorldUseCase exportUseCase;

    public WorldExportController(ExportWorldUseCase exportUseCase) {
        this.exportUseCase = exportUseCase;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> export(@PathVariable UUID worldId) {
        WorldExportBundle bundle = exportUseCase.export(worldId);
        String filename = "world-" + slug(bundle.worldName()) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(bundle.data());
    }

    private static String slug(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        return s.isEmpty() ? "export" : s;
    }
}
