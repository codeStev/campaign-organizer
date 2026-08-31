package com.campaignorganizer.campaign.adapter.clock.out.persistence;

import com.campaignorganizer.campaign.application.clock.port.out.ClockRepositoryPort;
import com.campaignorganizer.campaign.domain.clock.GameClock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ClockPersistenceAdapter implements ClockRepositoryPort {

    private final ClockJpaRepository repository;
    private final ClockPersistenceMapper mapper;

    public ClockPersistenceAdapter(ClockJpaRepository repository, ClockPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<GameClock> findByCampaign(UUID campaignId) {
        return repository.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GameClock> findByIdAndCampaign(UUID clockId, UUID campaignId) {
        return repository.findByIdAndCampaignId(clockId, campaignId).map(mapper::toDomain);
    }

    @Override
    public Optional<GameClock> findById(UUID clockId) {
        return repository.findById(clockId).map(mapper::toDomain);
    }

    @Override
    public GameClock save(GameClock clock) {
        return mapper.toDomain(repository.save(mapper.toEntity(clock)));
    }

    @Override
    public void delete(GameClock clock) {
        repository.deleteById(clock.getId());
    }
}
