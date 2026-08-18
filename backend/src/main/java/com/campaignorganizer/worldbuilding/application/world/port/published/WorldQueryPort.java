package com.campaignorganizer.worldbuilding.application.world.port.published;

import java.util.Optional;
import java.util.UUID;

/** Published port: world existence and read model for every other context (ADR-0050). */
public interface WorldQueryPort {

    boolean exists(UUID worldId);

    Optional<WorldView> findById(UUID worldId);
}
