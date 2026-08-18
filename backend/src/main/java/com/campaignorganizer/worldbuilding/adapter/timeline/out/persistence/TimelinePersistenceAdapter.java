package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import com.campaignorganizer.worldbuilding.application.timeline.port.out.TimelineRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.timeline.Timeline;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelinePersistenceAdapter implements TimelineRepositoryPort {

    private final TimelineJpaRepository repository;
    private final TimelinePersistenceMapper mapper;

    public TimelinePersistenceAdapter(TimelineJpaRepository repository, TimelinePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Timeline> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Timeline> findByIdAndWorld(UUID timelineId, UUID worldId) {
        return repository.findByIdAndWorldId(timelineId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<Timeline> findById(UUID timelineId) {
        return repository.findById(timelineId).map(mapper::toDomain);
    }

    @Override
    public Timeline save(Timeline timeline) {
        return mapper.toDomain(repository.save(mapper.toEntity(timeline)));
    }

    @Override
    public void delete(Timeline timeline) {
        repository.deleteById(timeline.getId());
    }
}
