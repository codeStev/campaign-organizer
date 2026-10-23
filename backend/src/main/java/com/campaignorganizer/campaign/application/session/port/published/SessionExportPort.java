package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published session access for the companion export (ADR-0121). The caller has already established
 * that the campaign belongs to the account.
 */
public interface SessionExportPort {

    List<SessionExportView> listForExport(UUID campaignId);

    /** {@code sessionNumber == null} numbers the session after the campaign's highest one. */
    SessionExportView createForExport(UUID campaignId, String title, LocalDate date, Integer sessionNumber);

    /**
     * Writes the summary unless that would overwrite one the caller did not write
     * ({@code ConflictException}): see {@code Session#writeSummary}.
     */
    SessionExportView writeSummary(UUID campaignId, UUID sessionId, String summary, Instant expectedUpdatedAt);
}
