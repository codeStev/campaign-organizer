package com.campaignorganizer.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.interchange.export.application.port.in.ImportBackupUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.DeleteWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test for the zip-unpacking/dispatch orchestration and the two import modes. */
@ExtendWith(MockitoExtension.class)
class BackupImportServiceTest {

    @Mock
    private ListWorldsUseCase listWorlds;
    @Mock
    private DeleteWorldUseCase deleteWorld;
    @Mock
    private ImportBackupUseCase importUseCase;

    private BackupImportService service() {
        return new BackupImportService(listWorlds, deleteWorld, importUseCase);
    }

    private InputStream zipWithOneWorld(UUID worldId, UUID mediaId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write("{}".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("worlds/" + worldId + ".json"));
            zip.write(("{\"world\":{\"id\":\"" + worldId + "\"}}").getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("media/" + worldId + "/" + mediaId));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Test
    void additiveModeNeverDeletesExistingWorlds() throws Exception {
        UUID worldId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        service().importBackup(zipWithOneWorld(worldId, mediaId), ImportMode.ADDITIVE);

        verify(deleteWorld, never()).delete(any());
        verify(importUseCase).importWorld(any(), any());
    }

    @Test
    void overwriteModeDeletesEveryExistingWorldFirst() throws Exception {
        UUID worldId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID existingA = UUID.randomUUID();
        UUID existingB = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(listWorlds.list()).thenReturn(List.of(
                new WorldView(existingA, "A", null, Map.of(), false, now, now),
                new WorldView(existingB, "B", null, Map.of(), false, now, now)));

        service().importBackup(zipWithOneWorld(worldId, mediaId), ImportMode.OVERWRITE);

        verify(deleteWorld, times(1)).delete(existingA);
        verify(deleteWorld, times(1)).delete(existingB);
        verify(importUseCase).importWorld(any(), any());
    }

    @Test
    void mediaBytesArePassedThroughKeyedByOriginalId() throws Exception {
        UUID worldId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        service().importBackup(zipWithOneWorld(worldId, mediaId), ImportMode.ADDITIVE);

        verify(importUseCase).importWorld(any(), argThat((Map<UUID, byte[]> media) ->
                media.containsKey(mediaId) && media.get(mediaId).length == 3));
    }
}
