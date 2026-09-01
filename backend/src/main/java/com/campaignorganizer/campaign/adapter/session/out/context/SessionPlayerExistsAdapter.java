package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.campaign.application.player.port.published.PlayerQueryPort;
import com.campaignorganizer.campaign.application.session.port.out.PlayerExistsPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves player existence/name for the session attendance module via the player query port. */
@Component
public class SessionPlayerExistsAdapter implements PlayerExistsPort {

    private final PlayerQueryPort players;

    public SessionPlayerExistsAdapter(PlayerQueryPort players) {
        this.players = players;
    }

    @Override
    public boolean existsInWorld(UUID playerId, UUID worldId) {
        return players.existsInWorld(playerId, worldId);
    }

    @Override
    public Optional<String> findName(UUID playerId, UUID worldId) {
        return players.findByIdInWorld(playerId, worldId).map(view -> view.name());
    }
}
