package com.campaignorganizer.media.adapter.out.persistence;

import com.campaignorganizer.media.application.port.out.MediaRepositoryPort;
import com.campaignorganizer.media.domain.MediaAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the media repository port. */
@Component
public class MediaPersistenceAdapter implements MediaRepositoryPort {

    private final MediaJpaRepository repository;
    private final MediaPersistenceMapper mapper;

    public MediaPersistenceAdapter(MediaJpaRepository repository, MediaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MediaAsset> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MediaAsset> findByIdAndWorld(UUID mediaId, UUID worldId) {
        return repository.findByIdAndWorldId(mediaId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<MediaAsset> findById(UUID mediaId) {
        return repository.findById(mediaId).map(mapper::toDomain);
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        return mapper.toDomain(repository.save(mapper.toEntity(asset)));
    }

    @Override
    public void delete(MediaAsset asset) {
        repository.deleteById(asset.getId());
    }
}
