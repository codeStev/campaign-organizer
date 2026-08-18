package com.campaignorganizer.whiteboard.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteboardJpaRepository extends JpaRepository<WhiteboardJpaEntity, UUID> {

    List<WhiteboardJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<WhiteboardJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
