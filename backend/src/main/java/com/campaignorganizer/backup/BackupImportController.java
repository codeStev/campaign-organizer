package com.campaignorganizer.backup;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Accepts a backup ZIP (produced by {@link BackupController}) and imports it (ADR-0061). */
@RestController
public class BackupImportController {

    private final BackupImportService importService;

    public BackupImportController(BackupImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/api/backup/import")
    public ResponseEntity<Void> importBackup(@RequestParam("mode") ImportMode mode,
            @RequestParam("file") MultipartFile file) throws IOException {
        importService.importBackup(file.getInputStream(), mode);
        return ResponseEntity.noContent().build();
    }
}
