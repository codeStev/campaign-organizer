package com.campaignorganizer.backup;

import com.campaignorganizer.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 * Composes the instance backup ZIP (ADR-0055): a {@code pg_dump} of the whole
 * database plus every file under the media volume. Streamed directly to the
 * given output — never buffered in memory — so an arbitrarily large media
 * library doesn't blow up heap.
 */
@Service
public class BackupService {

    private final PgDumpRunner pgDumpRunner;
    private final Path mediaDir;

    public BackupService(PgDumpRunner pgDumpRunner, AppProperties appProperties) {
        this.pgDumpRunner = pgDumpRunner;
        this.mediaDir = Path.of(appProperties.media().dir()).toAbsolutePath().normalize();
    }

    public void writeBackup(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        writeDatabaseDump(zip);
        writeMediaFiles(zip);
        zip.finish();
    }

    private void writeDatabaseDump(ZipOutputStream zip) throws IOException {
        zip.putNextEntry(new ZipEntry("database.sql"));
        try (InputStream dump = pgDumpRunner.dump()) {
            dump.transferTo(zip);
        }
        zip.closeEntry();
    }

    private void writeMediaFiles(ZipOutputStream zip) throws IOException {
        if (!Files.isDirectory(mediaDir)) {
            return;
        }
        try (var paths = Files.walk(mediaDir)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String relative = mediaDir.relativize(path).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry("media/" + relative));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }
}
