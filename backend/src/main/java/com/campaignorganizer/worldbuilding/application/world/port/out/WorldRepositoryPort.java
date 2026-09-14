package com.campaignorganizer.worldbuilding.application.world.port.out;

import com.campaignorganizer.worldbuilding.domain.world.World;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldRepositoryPort {

    List<World> findAllOrderByCreatedAtDesc();

    List<World> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<World> findById(UUID worldId);

    boolean existsById(UUID worldId);

    World save(World world);

    void delete(World world);

    /** One-time backfill only (ADR-0109). */
    void assignUnownedTo(UUID ownerId);
}
