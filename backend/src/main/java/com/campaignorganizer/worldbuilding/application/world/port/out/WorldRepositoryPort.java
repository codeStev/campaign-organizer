package com.campaignorganizer.worldbuilding.application.world.port.out;

import com.campaignorganizer.worldbuilding.domain.world.World;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldRepositoryPort {

    List<World> findAllOrderByCreatedAtDesc();

    Optional<World> findById(UUID worldId);

    boolean existsById(UUID worldId);

    World save(World world);

    void delete(World world);
}
