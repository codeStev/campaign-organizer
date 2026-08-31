package com.campaignorganizer.campaign.application.clock.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read clocks from sibling aggregates and other contexts (packet, export). */
public interface ClockQueryPort {

    List<ClockView> findByCampaign(UUID campaignId);

    Optional<ClockView> findById(UUID clockId);
}
