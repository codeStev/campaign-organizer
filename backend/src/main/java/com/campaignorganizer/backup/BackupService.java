package com.campaignorganizer.backup;

import com.campaignorganizer.interchange.export.application.port.in.ExportWorldUseCase;
import com.campaignorganizer.interchange.export.application.port.in.WorldExportBundle;
import com.campaignorganizer.media.application.port.in.ListMediaUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase.MediaContent;
import com.campaignorganizer.media.application.port.in.MediaView;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 * Composes the instance backup ZIP (ADR-0061): one JSON bundle per world
 * (world content, ADR-0055's FR-22 shape) plus that world's media files, and
 * a top-level manifest. This bounded-context-agnostic package (like
 * {@code auth}/{@code config}) is exempt from the published-ports rule
 * (ArchitectureTest), so it composes {@code interchange} and {@code media}
 * through their normal inbound use-case ports, the same way a controller
 * would.
 */
@Service
public class BackupService {

    private final ListWorldsUseCase worlds;
    private final ExportWorldUseCase exportUseCase;
    private final ListMediaUseCase listMedia;
    private final LoadMediaContentUseCase loadMedia;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BackupService(ListWorldsUseCase worlds, ExportWorldUseCase exportUseCase,
            ListMediaUseCase listMedia, LoadMediaContentUseCase loadMedia, ObjectMapper objectMapper,
            Clock clock) {
        this.worlds = worlds;
        this.exportUseCase = exportUseCase;
        this.listMedia = listMedia;
        this.loadMedia = loadMedia;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void writeBackup(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        List<WorldView> allWorlds = worlds.list();
        for (WorldView world : allWorlds) {
            writeWorld(zip, world.id());
        }
        writeManifest(zip, allWorlds);
        zip.finish();
    }

    private void writeWorld(ZipOutputStream zip, UUID worldId) throws IOException {
        WorldExportBundle bundle = exportUseCase.export(worldId);
        List<MediaView> media = listMedia.list(worldId);

        Map<String, Object> data = new LinkedHashMap<>(bundle.data());
        data.put("media", media);
        writeJsonEntry(zip, "worlds/" + worldId + ".json", data);

        for (MediaView asset : media) {
            MediaContent content = loadMedia.load(asset.id());
            zip.putNextEntry(new ZipEntry("media/" + worldId + "/" + asset.id()));
            zip.write(content.bytes());
            zip.closeEntry();
        }
    }

    private void writeManifest(ZipOutputStream zip, List<WorldView> allWorlds) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("exportedAt", clock.instant().toString());
        manifest.put("worldIds", allWorlds.stream().map(WorldView::id).toList());
        writeJsonEntry(zip, "manifest.json", manifest);
    }

    private void writeJsonEntry(ZipOutputStream zip, String name, Object value) throws IOException {
        // writeValueAsBytes, not writeValue(zip, ...): the latter closes the
        // underlying stream after writing (Jackson's default AUTO_CLOSE_TARGET),
        // which would kill the zip after the first entry.
        zip.putNextEntry(new ZipEntry(name));
        zip.write(objectMapper.writeValueAsBytes(value));
        zip.closeEntry();
    }
}
