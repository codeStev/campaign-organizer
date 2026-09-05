package com.campaignorganizer.interchange.calendar.application.port.out;

import com.campaignorganizer.interchange.calendar.domain.CalendarFeed;
import java.util.Optional;
import java.util.UUID;

public interface CalendarFeedRepositoryPort {

    Optional<CalendarFeed> findByCampaignId(UUID campaignId);

    Optional<CalendarFeed> findByToken(UUID token);

    CalendarFeed save(CalendarFeed feed);
}
