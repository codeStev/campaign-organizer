package com.campaignorganizer.campaign.application.session.port.in;

import java.util.UUID;

public interface DeleteSessionTranscriptUseCase {

    void delete(UUID worldId, UUID campaignId, UUID sessionId);
}
