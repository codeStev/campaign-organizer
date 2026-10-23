package com.campaignorganizer.interchange.companion.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A session as the companion export may see it: never summary or note text (ADR-0121). */
public record CompanionSession(UUID id, String title, Integer sessionNumber, LocalDate date, boolean hasSummary,
                               Instant updatedAt) {
}
