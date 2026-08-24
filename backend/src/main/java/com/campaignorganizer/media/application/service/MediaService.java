package com.campaignorganizer.media.application.service;

import com.campaignorganizer.media.application.port.in.DeleteMediaUseCase;
import com.campaignorganizer.media.application.port.in.ListMediaUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase;
import com.campaignorganizer.media.application.port.in.MediaView;
import com.campaignorganizer.media.application.port.in.UploadMediaUseCase;
import com.campaignorganizer.media.application.port.out.MediaRepositoryPort;
import com.campaignorganizer.media.application.port.out.MediaStoragePort;
import com.campaignorganizer.media.application.port.out.WorldExistsPort;
import com.campaignorganizer.media.application.port.published.MediaImportPort;
import com.campaignorganizer.media.application.port.published.MediaLookupPort;
import com.campaignorganizer.media.domain.MediaAsset;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Media use cases; also implements the published lookup port for other contexts. */
@Service
public class MediaService implements UploadMediaUseCase, ListMediaUseCase, DeleteMediaUseCase,
        LoadMediaContentUseCase, MediaLookupPort, MediaImportPort {

    private final MediaRepositoryPort media;
    private final MediaStoragePort storage;
    private final WorldExistsPort worlds;
    private final MediaViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public MediaService(MediaRepositoryPort media, MediaStoragePort storage, WorldExistsPort worlds,
                        MediaViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.media = media;
        this.storage = storage;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MediaView upload(UploadMediaCommand command) {
        requireWorld(command.worldId());
        long size = command.content() == null ? 0 : command.content().length;
        // Validate before spending storage.
        MediaAsset.ensureUploadable(command.contentType(), size);
        String key = storage.store(command.content());
        MediaAsset asset = MediaAsset.register(ids.newId(), command.worldId(), command.filename(),
                command.contentType(), size, key, clock.instant());
        return viewMapper.toView(media.save(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaView> list(UUID worldId) {
        requireWorld(worldId);
        return media.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID mediaId) {
        MediaAsset asset = media.findByIdAndWorld(mediaId, worldId)
                .orElseThrow(() -> new NotFoundException("Media not found"));
        media.delete(asset);
        storage.delete(asset.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaContent load(UUID mediaId) {
        MediaAsset asset = media.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found"));
        byte[] bytes = storage.load(asset.getStorageKey())
                .orElseThrow(() -> new NotFoundException("Media bytes missing"));
        return new MediaContent(asset.getContentType(), bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID mediaId, UUID worldId) {
        return media.findByIdAndWorld(mediaId, worldId).isPresent();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public void importMedia(UUID id, UUID worldId, String filename, String contentType, byte[] content,
            Instant createdAt) {
        String key = storage.store(content);
        MediaAsset asset = MediaAsset.reconstitute(id, worldId, filename, contentType, content.length,
                key, createdAt);
        media.save(asset);
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
