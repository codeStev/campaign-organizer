package com.campaignorganizer.campaign.application.clock.port.out;

import com.campaignorganizer.campaign.domain.clock.GameClock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClockRepositoryPort {

    List<GameClock> findByCampaign(UUID campaignId);

    Optional<GameClock> findByIdAndCampaign(UUID clockId, UUID campaignId);

    Optional<GameClock> findById(UUID clockId);

    GameClock save(GameClock clock);

    void delete(GameClock clock);
}
