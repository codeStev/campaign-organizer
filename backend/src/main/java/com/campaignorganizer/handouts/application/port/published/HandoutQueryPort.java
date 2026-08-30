package com.campaignorganizer.handouts.application.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cross-context read access to handouts (published; ADR-0050). */
public interface HandoutQueryPort {

    boolean existsInWorld(UUID handoutId, UUID worldId);

    Optional<HandoutView> findByIdInWorld(UUID handoutId, UUID worldId);

    Optional<HandoutView> findById(UUID handoutId);

    List<HandoutView> findByWorld(UUID worldId);

    List<HandoutView> findBySession(UUID sessionId);
}
