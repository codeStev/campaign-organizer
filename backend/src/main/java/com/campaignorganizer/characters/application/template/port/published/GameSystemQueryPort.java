package com.campaignorganizer.characters.application.template.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read game systems from sibling aggregates and other contexts. */
public interface GameSystemQueryPort {

    List<GameSystemView> findAll();

    Optional<GameSystemView> findById(UUID systemId);

    boolean existsById(UUID systemId);
}
