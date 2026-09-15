package com.campaignorganizer.worldbuilding.application.world.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: world existence and read model for every other context (ADR-0050). */
public interface WorldQueryPort {

    boolean exists(UUID worldId);

    Optional<WorldView> findById(UUID worldId);

    /** Every world the given account owns, for the global landing page (issue #68). */
    List<WorldView> findByOwner(UUID ownerId);
}
