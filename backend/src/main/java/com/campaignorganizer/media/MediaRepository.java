package com.campaignorganizer.media;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaAsset, UUID> {

    List<MediaAsset> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<MediaAsset> findByIdAndWorldId(UUID id, UUID worldId);
}
