package com.campaignorganizer.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.campaignorganizer.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the ZIP composition logic only — no real Postgres or
 * {@code pg_dump} binary involved (ADR-0055); {@link PgDumpRunner} is faked.
 */
class BackupServiceTest {

    private static final byte[] FAKE_DUMP = "FAKE PG_DUMP BYTES".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path mediaDir;

    private BackupService service() {
        PgDumpRunner fakeRunner = () -> new ByteArrayInputStream(FAKE_DUMP);
        AppProperties props = new AppProperties(
                "pw",
                new AppProperties.Jwt("unit-test-secret-that-is-at-least-32-bytes-long", 1),
                new AppProperties.Media(mediaDir.toString()));
        return new BackupService(fakeRunner, props);
    }

    private Map<String, byte[]> readZip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
            }
        }
        return entries;
    }

    @Test
    void backupContainsDatabaseDumpAndMediaFiles() throws IOException {
        Files.writeString(mediaDir.resolve("cover.png"), "image-bytes");
        Path sub = Files.createDirectory(mediaDir.resolve("sub"));
        Files.writeString(sub.resolve("map.jpg"), "nested-image-bytes");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().writeBackup(out);
        Map<String, byte[]> entries = readZip(out.toByteArray());

        assertThat(entries).containsKey("database.sql");
        assertThat(entries.get("database.sql")).isEqualTo(FAKE_DUMP);
        assertThat(entries).containsKey("media/cover.png");
        assertThat(new String(entries.get("media/cover.png"), StandardCharsets.UTF_8)).isEqualTo("image-bytes");
        assertThat(entries).containsKey("media/sub/map.jpg");
        assertThat(new String(entries.get("media/sub/map.jpg"), StandardCharsets.UTF_8))
                .isEqualTo("nested-image-bytes");
    }

    @Test
    void backupWorksWithNoMediaFiles() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().writeBackup(out);
        Map<String, byte[]> entries = readZip(out.toByteArray());

        assertThat(entries).containsOnlyKeys("database.sql");
    }

    @Test
    void propagatesIOExceptionFromDumpRunner() {
        PgDumpRunner failingRunner = () -> {
            throw new IOException("pg_dump not found");
        };
        AppProperties props = new AppProperties(
                "pw",
                new AppProperties.Jwt("unit-test-secret-that-is-at-least-32-bytes-long", 1),
                new AppProperties.Media(mediaDir.toString()));
        BackupService service = new BackupService(failingRunner, props);

        assertThat(catchThrowable(() -> service.writeBackup(new ByteArrayOutputStream())))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("pg_dump not found");
    }
}
