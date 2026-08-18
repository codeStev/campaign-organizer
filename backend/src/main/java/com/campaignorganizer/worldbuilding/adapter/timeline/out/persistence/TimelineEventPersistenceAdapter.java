package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import com.campaignorganizer.worldbuilding.application.timeline.port.out.TimelineEventRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.timeline.TimelineEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelineEventPersistenceAdapter implements TimelineEventRepositoryPort {

    private final TimelineEventJpaRepository repository;
    private final TimelineEventPersistenceMapper mapper;

    public TimelineEventPersistenceAdapter(TimelineEventJpaRepository repository,
                                           TimelineEventPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<TimelineEvent> findByTimelineOrdered(UUID timelineId) {
        return repository.findOrdered(timelineId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TimelineEvent> findByArticle(UUID articleId) {
        return repository.findByArticleId(articleId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TimelineEvent> findByIdAndTimeline(UUID eventId, UUID timelineId) {
        return repository.findByIdAndTimelineId(eventId, timelineId).map(mapper::toDomain);
    }

    @Override
    public TimelineEvent save(TimelineEvent event) {
        return mapper.toDomain(repository.save(mapper.toEntity(event)));
    }

    @Override
    public void delete(TimelineEvent event) {
        repository.deleteById(event.getId());
    }
}
