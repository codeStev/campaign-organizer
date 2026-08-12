package com.campaignorganizer.whiteboard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteboardRepository extends JpaRepository<Whiteboard, UUID> {

    List<Whiteboard> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<Whiteboard> findByIdAndWorldId(UUID id, UUID worldId);
}
