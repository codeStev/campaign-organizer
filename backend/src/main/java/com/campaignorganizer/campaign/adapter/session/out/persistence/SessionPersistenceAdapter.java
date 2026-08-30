package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.domain.session.Session;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SessionPersistenceAdapter implements SessionRepositoryPort {

    private final SessionJpaRepository repository;
    private final SessionPersistenceMapper mapper;

    public SessionPersistenceAdapter(SessionJpaRepository repository, SessionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Session> findOrdered(UUID campaignId) {
        return repository.findOrdered(campaignId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Session> findByIdAndCampaign(UUID sessionId, UUID campaignId) {
        return repository.findByIdAndCampaignId(sessionId, campaignId).map(mapper::toDomain);
    }

    @Override
    public Optional<Session> findById(UUID sessionId) {
        return repository.findById(sessionId).map(mapper::toDomain);
    }

    @Override
    public Session save(Session session) {
        return mapper.toDomain(repository.save(mapper.toEntity(session)));
    }

    @Override
    public void delete(Session session) {
        repository.deleteById(session.getId());
    }
}
