package com.campaignorganizer.map;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapPinRepository extends JpaRepository<MapPin, UUID> {

    List<MapPin> findByMapIdOrderByCreatedAtAsc(UUID mapId);

    Optional<MapPin> findByIdAndMapId(UUID id, UUID mapId);
}
