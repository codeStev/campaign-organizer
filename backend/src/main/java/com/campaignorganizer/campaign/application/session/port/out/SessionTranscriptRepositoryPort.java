package com.campaignorganizer.campaign.application.session.port.out;

import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import java.util.Optional;
import java.util.UUID;

public interface SessionTranscriptRepositoryPort {

    Optional<SessionTranscript> findBySession(UUID sessionId);

    SessionTranscript save(SessionTranscript transcript);

    void delete(SessionTranscript transcript);
}
