package com.campaignorganizer.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campaignorganizer.interchange.export.application.port.in.ExportWorldUseCase;
import com.campaignorganizer.interchange.export.application.port.in.WorldExportBundle;
import com.campaignorganizer.media.application.port.in.ListMediaUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase.MediaContent;
import com.campaignorganizer.media.application.port.in.MediaView;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import tools.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test for the ZIP composition logic — every collaborator mocked, no DB/disk. */
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private ListWorldsUseCase listWorlds;
    @Mock
    private ExportWorldUseCase exportUseCase;
    @Mock
    private ListMediaUseCase listMedia;
    @Mock
    private LoadMediaContentUseCase loadMedia;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BackupService service() {
        return new BackupService(listWorlds, exportUseCase, listMedia, loadMedia, objectMapper);
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
    void backupContainsManifestWorldJsonAndMediaFiles() throws IOException {
        UUID worldId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        WorldView world = new WorldView(worldId, "Dark Caribbean", null, Map.of(),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        Map<String, Object> bundleData = new LinkedHashMap<>();
        bundleData.put("exportVersion", 1);
        bundleData.put("world", world);
        MediaView mediaView = new MediaView(mediaId, worldId, "cover.png", "image/png", 3,
                Instant.parse("2026-01-01T00:00:00Z"));

        when(listWorlds.list()).thenReturn(List.of(world));
        when(exportUseCase.export(worldId)).thenReturn(new WorldExportBundle(world.name(), bundleData));
        when(listMedia.list(worldId)).thenReturn(List.of(mediaView));
        when(loadMedia.load(mediaId)).thenReturn(new MediaContent("image/png", new byte[] {1, 2, 3}));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().writeBackup(out);
        Map<String, byte[]> entries = readZip(out.toByteArray());

        assertThat(entries).containsKey("manifest.json");
        assertThat(new String(entries.get("manifest.json"), StandardCharsets.UTF_8))
                .contains(worldId.toString());
        assertThat(entries).containsKey("worlds/" + worldId + ".json");
        assertThat(new String(entries.get("worlds/" + worldId + ".json"), StandardCharsets.UTF_8))
                .contains("Dark Caribbean")
                .contains("\"media\"");
        assertThat(entries.get("media/" + worldId + "/" + mediaId)).isEqualTo(new byte[] {1, 2, 3});
    }

    @Test
    void backupWithNoWorldsIsJustAManifest() throws IOException {
        when(listWorlds.list()).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().writeBackup(out);
        Map<String, byte[]> entries = readZip(out.toByteArray());

        assertThat(entries).containsOnlyKeys("manifest.json");
    }
}
