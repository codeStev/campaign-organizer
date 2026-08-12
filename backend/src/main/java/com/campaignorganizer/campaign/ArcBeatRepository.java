package com.campaignorganizer.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcBeatRepository extends JpaRepository<ArcBeat, UUID> {

    List<ArcBeat> findByArcIdOrderByPositionAscCreatedAtAsc(UUID arcId);

    Optional<ArcBeat> findByIdAndArcId(UUID id, UUID arcId);
}
