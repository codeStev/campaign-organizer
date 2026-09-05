package com.campaignorganizer.interchange.calendar.adapter.out.persistence;

import com.campaignorganizer.interchange.calendar.application.port.out.CalendarFeedRepositoryPort;
import com.campaignorganizer.interchange.calendar.domain.CalendarFeed;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CalendarFeedPersistenceAdapter implements CalendarFeedRepositoryPort {

    private final CalendarFeedJpaRepository repository;
    private final CalendarFeedPersistenceMapper mapper;

    public CalendarFeedPersistenceAdapter(CalendarFeedJpaRepository repository,
                                          CalendarFeedPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CalendarFeed> findByCampaignId(UUID campaignId) {
        return repository.findById(campaignId).map(mapper::toDomain);
    }

    @Override
    public Optional<CalendarFeed> findByToken(UUID token) {
        return repository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public CalendarFeed save(CalendarFeed feed) {
        return mapper.toDomain(repository.save(mapper.toEntity(feed)));
    }
}
