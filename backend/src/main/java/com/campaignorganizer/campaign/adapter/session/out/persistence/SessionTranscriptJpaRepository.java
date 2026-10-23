package com.campaignorganizer.campaign.adapter.session.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTranscriptJpaRepository extends JpaRepository<SessionTranscriptJpaEntity, UUID> {

    Optional<SessionTranscriptJpaEntity> findBySessionId(UUID sessionId);
}
