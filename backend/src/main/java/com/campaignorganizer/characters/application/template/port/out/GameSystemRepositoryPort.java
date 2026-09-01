package com.campaignorganizer.characters.application.template.port.out;

import com.campaignorganizer.characters.domain.template.GameSystem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSystemRepositoryPort {

    List<GameSystem> findAll();

    Optional<GameSystem> findById(UUID systemId);

    Optional<GameSystem> findByNameIgnoreCase(String name);

    GameSystem save(GameSystem system);

    void delete(GameSystem system);
}
