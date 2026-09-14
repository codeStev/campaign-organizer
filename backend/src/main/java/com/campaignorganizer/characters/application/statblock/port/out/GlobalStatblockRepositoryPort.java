package com.campaignorganizer.characters.application.statblock.port.out;

import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlobalStatblockRepositoryPort {

    List<GlobalStatblock> findAllByOwnerId(UUID ownerId);

    List<GlobalStatblock> findBySystemId(UUID systemId);

    Optional<GlobalStatblock> findById(UUID globalStatblockId);

    Optional<GlobalStatblock> findBySystemIdAndName(UUID systemId, String name);

    boolean existsByGlobalTemplateId(UUID globalTemplateId);

    GlobalStatblock save(GlobalStatblock statblock);

    void delete(GlobalStatblock statblock);

    /** One-time backfill only (ADR-0109). */
    void assignUnownedTo(UUID ownerId);
}
