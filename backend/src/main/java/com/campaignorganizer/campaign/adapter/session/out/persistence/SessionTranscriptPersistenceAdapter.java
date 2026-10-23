package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.application.session.port.out.SessionTranscriptRepositoryPort;
import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the session transcript repository port. */
@Component
public class SessionTranscriptPersistenceAdapter implements SessionTranscriptRepositoryPort {

    private final SessionTranscriptJpaRepository repository;
    private final SessionTranscriptPersistenceMapper mapper;

    public SessionTranscriptPersistenceAdapter(SessionTranscriptJpaRepository repository,
                                               SessionTranscriptPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SessionTranscript> findBySession(UUID sessionId) {
        return repository.findBySessionId(sessionId).map(mapper::toDomain);
    }

    @Override
    public SessionTranscript save(SessionTranscript transcript) {
        return mapper.toDomain(repository.save(mapper.toEntity(transcript)));
    }

    @Override
    public void delete(SessionTranscript transcript) {
        repository.deleteById(transcript.getId());
    }
}
