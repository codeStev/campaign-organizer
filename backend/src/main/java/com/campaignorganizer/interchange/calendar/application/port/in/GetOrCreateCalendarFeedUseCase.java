package com.campaignorganizer.interchange.calendar.application.port.in;

import java.util.UUID;

/** Returns a campaign's subscription token, minting one on first use. */
public interface GetOrCreateCalendarFeedUseCase {

    UUID getOrCreateToken(UUID worldId, UUID campaignId);
}
