package com.campaignorganizer.campaign.application.session.port.in;

import java.util.UUID;

public interface DeleteCheatSheetUseCase {

    /** Idempotent: deleting a missing sheet is a no-op. */
    void delete(UUID worldId, UUID campaignId, UUID sessionId);
}
