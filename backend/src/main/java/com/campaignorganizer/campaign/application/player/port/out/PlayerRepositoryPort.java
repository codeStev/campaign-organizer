package com.campaignorganizer.campaign.application.player.port.out;

import com.campaignorganizer.campaign.domain.player.Player;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepositoryPort {

    List<Player> findByWorld(UUID worldId);

    Optional<Player> findByIdAndWorld(UUID playerId, UUID worldId);

    Optional<Player> findById(UUID playerId);

    boolean existsInWorld(UUID playerId, UUID worldId);

    Player save(Player player);

    void delete(Player player);
}
