package com.campaignorganizer.backup;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Serves a full instance backup (database + media) as a downloadable ZIP (ADR-0055). */
@RestController
public class BackupController {

    private static final DateTimeFormatter FILENAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/api/backup")
    public ResponseEntity<StreamingResponseBody> backup() {
        String filename = "campaign-organizer-backup-" + FILENAME_TIMESTAMP.format(OffsetDateTime.now()) + ".zip";
        StreamingResponseBody body = out -> backupService.writeBackup(out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.valueOf("application/zip"))
                .body(body);
    }
}
