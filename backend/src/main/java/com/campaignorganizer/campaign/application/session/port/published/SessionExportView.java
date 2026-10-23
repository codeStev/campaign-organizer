package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the companion export may see of a session (ADR-0121): never the summary or notes text, only
 * whether a summary exists and when the session last changed.
 */
public record SessionExportView(UUID id, String title, Integer sessionNumber, LocalDate date,
                                boolean hasSummary, Instant updatedAt) {
}
