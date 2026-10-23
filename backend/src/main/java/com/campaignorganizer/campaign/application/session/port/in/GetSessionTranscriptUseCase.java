package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import java.util.UUID;

public interface GetSessionTranscriptUseCase {

    /** Never null: a view with {@code id == null} and no lines when no transcript was saved yet. */
    SessionTranscriptView get(UUID worldId, UUID campaignId, UUID sessionId);
}
