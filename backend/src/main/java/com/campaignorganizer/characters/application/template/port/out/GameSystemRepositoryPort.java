package com.campaignorganizer.characters.application.template.port.out;

import com.campaignorganizer.characters.domain.template.GameSystem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSystemRepositoryPort {

    List<GameSystem> findAllByOwnerId(UUID ownerId);

    Optional<GameSystem> findById(UUID systemId);

    Optional<GameSystem> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    GameSystem save(GameSystem system);

    void delete(GameSystem system);

    /** One-time backfill only (ADR-0109). */
    void assignUnownedTo(UUID ownerId);
}
