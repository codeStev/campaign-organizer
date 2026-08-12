package com.campaignorganizer.statblock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatblockRepository extends JpaRepository<Statblock, UUID> {

    List<Statblock> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<Statblock> findByIdAndWorldId(UUID id, UUID worldId);
}
