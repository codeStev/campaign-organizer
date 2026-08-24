package com.campaignorganizer.backup;

import com.campaignorganizer.interchange.export.application.port.in.ImportBackupUseCase;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.world.port.in.DeleteWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Service;

/**
 * Restores a backup ZIP produced by {@link BackupService} (ADR-0061). Reads
 * the whole upload into a temp file for random access (the zip's manifest,
 * per-world JSON, and per-world media all need to be cross-referenced), then
 * delegates each world to {@link ImportBackupUseCase}. Overwrite mode's
 * "delete everything first" step lives here, not in {@code interchange}:
 * that use case is a plain worldbuilding one, and this package is exempt
 * from the published-ports rule (ArchitectureTest), same as
 * {@link BackupService}.
 */
@Service
public class BackupImportService {

    private static final Pattern WORLD_ENTRY = Pattern.compile("^worlds/([0-9a-fA-F-]{36})\\.json$");

    private final ListWorldsUseCase listWorlds;
    private final DeleteWorldUseCase deleteWorld;
    private final ImportBackupUseCase importUseCase;

    public BackupImportService(ListWorldsUseCase listWorlds, DeleteWorldUseCase deleteWorld,
            ImportBackupUseCase importUseCase) {
        this.listWorlds = listWorlds;
        this.deleteWorld = deleteWorld;
        this.importUseCase = importUseCase;
    }

    public void importBackup(InputStream zipStream, ImportMode mode) throws IOException {
        Path temp = Files.createTempFile("backup-import-", ".zip");
        try {
            Files.copy(zipStream, temp, StandardCopyOption.REPLACE_EXISTING);
            try (ZipFile zip = new ZipFile(temp.toFile())) {
                requireAtLeastOneWorld(zip);
                if (mode == ImportMode.OVERWRITE) {
                    for (WorldView world : listWorlds.list()) {
                        deleteWorld.delete(world.id());
                    }
                }
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Matcher matcher = WORLD_ENTRY.matcher(entry.getName());
                    if (matcher.matches()) {
                        importWorldEntry(zip, entry, matcher.group(1));
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void importWorldEntry(ZipFile zip, ZipEntry entry, String worldId) throws IOException {
        byte[] worldJson;
        try (InputStream in = zip.getInputStream(entry)) {
            worldJson = in.readAllBytes();
        }
        Map<UUID, byte[]> mediaByOldId = readMediaFor(zip, worldId);
        importUseCase.importWorld(worldJson, mediaByOldId);
    }

    private Map<UUID, byte[]> readMediaFor(ZipFile zip, String worldId) throws IOException {
        String prefix = "media/" + worldId + "/";
        Map<UUID, byte[]> media = new HashMap<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                continue;
            }
            UUID mediaId = UUID.fromString(entry.getName().substring(prefix.length()));
            try (InputStream in = zip.getInputStream(entry)) {
                media.put(mediaId, in.readAllBytes());
            }
        }
        return media;
    }

    private void requireAtLeastOneWorld(ZipFile zip) {
        if (zip.getEntry("manifest.json") == null) {
            throw new ValidationException("Not a campaign-organizer backup (missing manifest.json)");
        }
    }
}
