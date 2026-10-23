package com.campaignorganizer.campaign.application.session.port.published;

import java.util.Optional;
import java.util.UUID;

/** Cross-context read access to session transcripts (published; used by the world export). */
public interface SessionTranscriptQueryPort {

    Optional<SessionTranscriptView> findBySession(UUID sessionId);
}
