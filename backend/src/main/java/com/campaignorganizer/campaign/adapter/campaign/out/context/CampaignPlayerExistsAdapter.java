package com.campaignorganizer.campaign.adapter.campaign.out.context;

import com.campaignorganizer.campaign.application.campaign.port.out.PlayerExistsPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves player existence/name for the campaign roster module via the player query port. */
@Component
public class CampaignPlayerExistsAdapter implements PlayerExistsPort {

    private final PlayerQueryPort players;

    public CampaignPlayerExistsAdapter(PlayerQueryPort players) {
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
