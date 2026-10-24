package com.campaignorganizer.interchange.companion.application.port.in;

import java.util.List;
import java.util.UUID;

/** The sessions of one of the account's campaigns, for the companion export (ADR-0121). */
public interface ListCompanionSessionsUseCase {

    List<CompanionSession> list(UUID accountId, UUID campaignId);
}
