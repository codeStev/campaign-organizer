package com.campaignorganizer.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

/** Unit tests for the nightly snapshot job (FR-42) — mocked BackupService, temp dirs. */
@ExtendWith(MockitoExtension.class)
class ScheduledBackupsTest {

    private static final Instant T0 = Instant.parse("2026-08-26T03:37:00Z");

    @Mock
    private BackupService backups;

    private final Clock clock = Clock.fixed(T0, ZoneOffset.UTC);

    private ScheduledBackups job(Path mediaDir, int keep) {
        return new ScheduledBackups(backups, mediaDir.toString(), keep, clock);
    }

    /** Stub writeBackup to emit recognizable bytes into whatever stream it gets. */
    private void writeBytes(byte[] bytes) throws IOException {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(0);
            out.write(bytes);
            return null;
        }).when(backups).writeBackup(any());
    }

    private Path backupDirOf(Path mediaDir) {
        return mediaDir.resolve("backups");
    }

    @Test
    void snapshotWritesATimestampedZipUnderMediaBackups(@TempDir Path mediaDir) throws IOException {
        writeBytes("zip".getBytes(StandardCharsets.UTF_8));
        Path written = job(mediaDir, 7).snapshot();

        assertThat(written).isEqualTo(backupDirOf(mediaDir).resolve("backup-2026-08-26_03-37-00.zip"));
        assertThat(written).hasContent("zip");
        // No temp leftovers.
        try (Stream<Path> files = Files.list(backupDirOf(mediaDir))) {
            assertThat(files.noneMatch(p -> p.toString().endsWith(".part"))).isTrue();
        }
    }

    @Test
    void snapshotPrunesOldestBeyondKeep(@TempDir Path mediaDir) throws IOException {
        writeBytes("zip".getBytes(StandardCharsets.UTF_8));
        Path dir = backupDirOf(mediaDir);
        Files.createDirectories(dir);
        for (int d = 1; d <= 3; d++) {
            Files.createFile(dir.resolve("backup-2026-08-2%d_03-37-00.zip".formatted(d)));
        }

        job(mediaDir, 2).snapshot();

        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(p -> p.getFileName().toString()).sorted().toList();
        }
        // The fresh 26th + the newest seed survive; the 21st and 22nd go.
        assertThat(names).containsExactly(
                "backup-2026-08-23_03-37-00.zip",
                "backup-2026-08-26_03-37-00.zip");
    }

    @Test
    void pruneTouchesOnlyItsOwnFiles(@TempDir Path mediaDir) throws IOException {
        writeBytes("zip".getBytes(StandardCharsets.UTF_8));
        Path dir = backupDirOf(mediaDir);
        Files.createDirectories(dir);
        for (int d = 1; d <= 4; d++) {
            Files.createFile(dir.resolve("backup-2026-08-2%d_03-37-00.zip".formatted(d)));
        }
        Files.createFile(dir.resolve("notes.txt"));
        Files.createFile(dir.resolve("media-asset.zip"));

        job(mediaDir, 1).snapshot();

        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(p -> p.getFileName().toString()).sorted().toList();
        }
        // Unrelated files stay; only old backup-* zips are removed.
        assertThat(names).containsExactlyInAnyOrder(
                "notes.txt", "media-asset.zip", "backup-2026-08-26_03-37-00.zip");
    }

    @Test
    void pruneSweepsStalePartFilesFromCrashedRuns(@TempDir Path mediaDir) throws IOException {
        writeBytes("zip".getBytes(StandardCharsets.UTF_8));
        Path dir = backupDirOf(mediaDir);
        Files.createDirectories(dir);
        Files.createFile(dir.resolve("backup-2026-08-20_03-37-00.zip.part"));

        job(mediaDir, 7).snapshot();

        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(p -> p.getFileName().toString()).toList();
        }
        assertThat(names).containsExactly("backup-2026-08-26_03-37-00.zip");
    }

    @Test
    void failedSnapshotLeavesNothingBehindAndPrunesNothing(@TempDir Path mediaDir) throws IOException {
        doThrow(new IOException("disk full")).when(backups).writeBackup(any());
        Path dir = backupDirOf(mediaDir);
        Files.createDirectories(dir);
        Files.createFile(dir.resolve("backup-2026-08-01_03-37-00.zip"));

        assertThatThrownBy(() -> job(mediaDir, 7).snapshot()).isInstanceOf(IOException.class);

        // The half-written .part is cleaned up; the previous snapshot survives.
        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(p -> p.getFileName().toString()).toList();
        }
        assertThat(names).containsExactly("backup-2026-08-01_03-37-00.zip");
    }

    @Test
    void nightlySwallowsFailuresSoTheScheduleKeepsFiring(@TempDir Path mediaDir) throws IOException {
        doThrow(new IOException("disk full")).when(backups).writeBackup(any());

        job(mediaDir, 7).nightly(); // must not throw

        assertThat(backupDirOf(mediaDir)).exists();
    }

    @Test
    void keepBelowOneIsRejected(@TempDir Path mediaDir) {
        assertThatThrownBy(() -> job(mediaDir, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** The default cron stays a valid nightly expression near 03:30 UTC. */
    @Test
    void defaultCronIsANightlyExpression() throws NoSuchMethodException {
        String raw = ScheduledBackups.class.getMethod("nightly")
                .getAnnotation(Scheduled.class).cron();
        String def = raw.substring(raw.indexOf(':') + 1, raw.indexOf('}'));
        CronExpression parsed = CronExpression.parse(def);
        java.time.LocalDateTime next = parsed.next(java.time.LocalDateTime.of(2026, 8, 26, 0, 0));
        assertThat(next.getHour()).isEqualTo(3);
        assertThat(next.getMinute()).isBetween(25, 45);
    }
}
