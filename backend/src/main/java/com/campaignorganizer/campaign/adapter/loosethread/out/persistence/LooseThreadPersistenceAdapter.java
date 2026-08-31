package com.campaignorganizer.campaign.adapter.loosethread.out.persistence;

import com.campaignorganizer.campaign.application.loosethread.port.out.LooseThreadRepositoryPort;
import com.campaignorganizer.campaign.domain.loosethread.LooseThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LooseThreadPersistenceAdapter implements LooseThreadRepositoryPort {

    private final LooseThreadJpaRepository repository;
    private final LooseThreadPersistenceMapper mapper;

    public LooseThreadPersistenceAdapter(LooseThreadJpaRepository repository,
                                         LooseThreadPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<LooseThread> findBySession(UUID sessionId) {
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<LooseThread> findByCampaign(UUID campaignId) {
        return repository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LooseThread> findByIdAndSession(UUID threadId, UUID sessionId) {
        return repository.findByIdAndSessionId(threadId, sessionId).map(mapper::toDomain);
    }

    @Override
    public Optional<LooseThread> findById(UUID threadId) {
        return repository.findById(threadId).map(mapper::toDomain);
    }

    @Override
    public LooseThread save(LooseThread thread) {
        return mapper.toDomain(repository.save(mapper.toEntity(thread)));
    }

    @Override
    public void delete(LooseThread thread) {
        repository.deleteById(thread.getId());
    }
}
