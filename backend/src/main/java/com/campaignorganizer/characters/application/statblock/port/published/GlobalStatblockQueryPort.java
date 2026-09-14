package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read the global statblock catalog from sibling contexts (ADR-0096). */
public interface GlobalStatblockQueryPort {

    List<GlobalStatblockView> findAll();

    /** Used by {@code WorldPermissionEvaluator} to check a referenced global statblock's owner (ADR-0109). */
    Optional<GlobalStatblockView> findById(UUID globalStatblockId);
}
