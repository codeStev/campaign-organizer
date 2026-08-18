package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Published read model for a session. */
public record SessionView(
        UUID id,
        UUID campaignId,
        String title,
        Integer sessionNumber,
        LocalDate date,
        String summary,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
