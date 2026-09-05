package com.campaignorganizer.interchange.calendar.application.port.in;

import java.util.UUID;

/** Mints a fresh subscription token, invalidating the previous URL. */
public interface RegenerateCalendarFeedUseCase {

    UUID regenerateToken(UUID worldId, UUID campaignId);
}
