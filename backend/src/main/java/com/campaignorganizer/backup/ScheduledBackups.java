package com.campaignorganizer.backup;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-42: nightly in-app backup snapshots. On a cron (env-overridable, default
 * once a night) writes a timestamped instance ZIP via
 * {@link BackupService#writeBackup(OutputStream)} into {@code backups/} under
 * the media volume, then prunes older snapshots keeping the most recent N.
 * Lives beside BackupService in this context-agnostic package for the same
 * ArchitectureTest exemption; snapshots land next to the data they preserve so
 * one volume mount covers everything.
 */
@Component
public class ScheduledBackups {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBackups.class);

    static final String FILE_PREFIX = "backup-";
    static final String FILE_SUFFIX = ".zip";
    private static final String PART_SUFFIX = ".part";
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final BackupService backups;
    private final Path backupDir;
    private final int keep;
    private final Clock clock;

    public ScheduledBackups(BackupService backups,
            @Value("${app.media.dir}") String mediaDir,
            @Value("${app.backup.keep:7}") int keep,
            Clock clock) {
        this.backups = backups;
        this.backupDir = Path.of(mediaDir, "backups");
        if (keep < 1) {
            throw new IllegalArgumentException("app.backup.keep must be >= 1");
        }
        this.keep = keep;
        this.clock = clock;
    }

    /** Snapshot now and return the file written; package-visible for tests. */
    synchronized Path snapshot() throws IOException {
        Files.createDirectories(backupDir);
        // UTC stamps: zero-padded, sort lexicographically == chronologically.
        String stamp = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).format(STAMP);
        Path target = backupDir.resolve(FILE_PREFIX + stamp + FILE_SUFFIX);
        // Write to a temp file first so a crashed run never leaves a truncated
        // ZIP that prune would happily keep as a "good" old snapshot.
        Path tmp = backupDir.resolve(target.getFileName() + PART_SUFFIX);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
            backups.writeBackup(out);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Wrote scheduled backup {}", target.getFileName());
        prune();
        return target;
    }

    /**
     * Nightly snapshot; failures are logged, not thrown — a broken night must
     * not stop the scheduler from firing again tomorrow.
     */
    @Scheduled(cron = "${app.backup.cron:0 37 3 * * *}")
    public void nightly() {
        try {
            snapshot();
        } catch (Exception e) {
            log.error("Scheduled backup failed", e);
        }
    }

    /** Delete oldest snapshots beyond {@code keep}; only ever touches our files. */
    private void prune() throws IOException {
        List<Path> zips = new ArrayList<>();
        List<Path> staleParts = new ArrayList<>();
        try (Stream<Path> files = Files.list(backupDir)) {
            files.map(Path::getFileName).forEach(n -> {
                String name = n.toString();
                if (name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX)) {
                    zips.add(backupDir.resolve(n));
                } else if (name.endsWith(PART_SUFFIX)) {
                    // Leftovers from a crashed run are worthless; sweep them.
                    staleParts.add(backupDir.resolve(n));
                }
            });
        }
        for (Path part : staleParts) {
            Files.deleteIfExists(part);
            log.info("Deleted incomplete backup {}", part.getFileName());
        }
        zips.sort(Path::compareTo);
        int over = zips.size() - keep;
        for (int i = 0; i < over; i++) {
            Files.deleteIfExists(zips.get(i));
            log.info("Pruned old backup {}", zips.get(i).getFileName());
        }
    }
}
