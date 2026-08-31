package com.campaignorganizer.campaign.application.clock.port.in;

import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import java.util.List;
import java.util.UUID;

public interface ListClocksUseCase {

    List<ClockView> list(UUID worldId, UUID campaignId);
}
