package com.campaignorganizer.media.application.port.out;

import com.campaignorganizer.media.domain.MediaAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepositoryPort {

    List<MediaAsset> findByWorld(UUID worldId);

    Optional<MediaAsset> findByIdAndWorld(UUID mediaId, UUID worldId);

    Optional<MediaAsset> findById(UUID mediaId);

    MediaAsset save(MediaAsset asset);

    void delete(MediaAsset asset);
}
