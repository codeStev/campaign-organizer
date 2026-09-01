package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for one session attendance row. */
public record SessionAttendanceView(UUID id, UUID sessionId, UUID playerId, boolean present,
                                    UUID characterId, Instant createdAt) {
}
