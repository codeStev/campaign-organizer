package com.campaignorganizer.interchange.companion.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/** Creates a session for a recording that has none yet (ADR-0121). */
public interface CreateCompanionSessionUseCase {

    CompanionSession create(CreateCompanionSessionCommand command);

    /** {@code sessionNumber == null} numbers the session after the campaign's highest one. */
    record CreateCompanionSessionCommand(UUID accountId, UUID campaignId, String title, LocalDate date,
                                         Integer sessionNumber) {
    }
}
