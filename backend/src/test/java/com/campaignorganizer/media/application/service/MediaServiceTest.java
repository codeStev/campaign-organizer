package com.campaignorganizer.media.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase.MediaContent;
import com.campaignorganizer.media.application.port.in.MediaView;
import com.campaignorganizer.media.application.port.in.UploadMediaUseCase.UploadMediaCommand;
import com.campaignorganizer.media.application.port.out.MediaRepositoryPort;
import com.campaignorganizer.media.application.port.out.MediaStoragePort;
import com.campaignorganizer.media.application.port.out.WorldExistsPort;
import com.campaignorganizer.media.domain.MediaAsset;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test with mocked out-ports — no Spring, no DB, no disk. */
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepositoryPort media;
    @Mock
    private MediaStoragePort storage;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final MediaViewMapper viewMapper = new MediaViewMapperImpl();

    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(media, storage, worlds, viewMapper, ids, clock);
    }

    @Test
    void uploadStoresBytesAndReturnsView() {
        UUID worldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        byte[] bytes = {1, 2, 3, 4};
        when(worlds.exists(worldId)).thenReturn(true);
        when(storage.store(bytes)).thenReturn("key-1");
        when(ids.newId()).thenReturn(newId);
        when(media.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MediaView view = service.upload(new UploadMediaCommand(worldId, "map.png", "image/png", bytes));

        assertThat(view.id()).isEqualTo(newId);
        assertThat(view.filename()).isEqualTo("map.png");
        assertThat(view.sizeBytes()).isEqualTo(4);
    }

    @Test
    void uploadRejectsNonImageWithoutStoring() {
        UUID worldId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(
                new UploadMediaCommand(worldId, "x.txt", "text/plain", new byte[] {1})))
                .isInstanceOf(ValidationException.class);
        verify(storage, never()).store(any());
    }

    @Test
    void loadMissingBytesThrowsNotFound() {
        UUID mediaId = UUID.randomUUID();
        MediaAsset asset = MediaAsset.reconstitute(mediaId, UUID.randomUUID(), "f.png", "image/png",
                4, "key-1", Instant.now());
        when(media.findById(mediaId)).thenReturn(Optional.of(asset));
        when(storage.load("key-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load(mediaId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesBothMetadataAndBytes() {
        UUID worldId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        MediaAsset asset = MediaAsset.reconstitute(mediaId, worldId, "f.png", "image/png",
                4, "key-1", Instant.now());
        when(media.findByIdAndWorld(mediaId, worldId)).thenReturn(Optional.of(asset));

        service.delete(worldId, mediaId);

        verify(media).delete(asset);
        verify(storage).delete("key-1");
    }

    @Test
    void loadContentReturnsBytes() {
        UUID mediaId = UUID.randomUUID();
        MediaAsset asset = MediaAsset.reconstitute(mediaId, UUID.randomUUID(), "f.png", "image/png",
                4, "key-1", Instant.now());
        when(media.findById(mediaId)).thenReturn(Optional.of(asset));
        when(storage.load("key-1")).thenReturn(Optional.of(new byte[] {9, 9}));

        MediaContent content = service.load(mediaId);

        assertThat(content.contentType()).isEqualTo("image/png");
        assertThat(content.bytes()).containsExactly(9, 9);
    }
}
