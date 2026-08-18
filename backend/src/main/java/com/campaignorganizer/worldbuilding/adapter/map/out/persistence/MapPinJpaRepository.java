package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapPinJpaRepository extends JpaRepository<MapPinJpaEntity, UUID> {

    List<MapPinJpaEntity> findByMapIdOrderByCreatedAtAsc(UUID mapId);

    List<MapPinJpaEntity> findByArticleId(UUID articleId);

    Optional<MapPinJpaEntity> findByIdAndMapId(UUID id, UUID mapId);
}
