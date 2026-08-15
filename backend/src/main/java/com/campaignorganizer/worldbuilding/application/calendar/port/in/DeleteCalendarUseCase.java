package com.campaignorganizer.worldbuilding.application.calendar.port.in;

import java.util.UUID;

public interface DeleteCalendarUseCase {

    void delete(UUID worldId, UUID calendarId);
}
