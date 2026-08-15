package com.campaignorganizer.worldbuilding.application.timeline.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.CreateTimelineUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.DeleteTimelineUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.ListTimelinesUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.CreateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.UpdateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.UpdateTimelineUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.TimelineRepositoryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineLookupPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import com.campaignorganizer.worldbuilding.domain.timeline.Timeline;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Timeline use cases; implements the published lookup port for consumers. */
@Service
public class TimelineService implements CreateTimelineUseCase, UpdateTimelineUseCase,
        DeleteTimelineUseCase, ListTimelinesUseCase, TimelineLookupPort {

    private final TimelineRepositoryPort timelines;
    private final WorldExistsPort worlds;
    private final CalendarQueryPort calendars;
    private final TimelineViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public TimelineService(TimelineRepositoryPort timelines, WorldExistsPort worlds,
                           CalendarQueryPort calendars, TimelineViewMapper viewMapper,
                           IdGenerator ids, Clock clock) {
        this.timelines = timelines;
        this.worlds = worlds;
        this.calendars = calendars;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TimelineView create(CreateTimelineCommand command) {
        requireWorld(command.worldId());
        requireCalendar(command.worldId(), command.calendarId());
        Timeline created = Timeline.create(ids.newId(), command.worldId(), command.name(),
                command.description(), command.calendarId(), clock.instant());
        return viewMapper.toView(timelines.save(created));
    }

    @Override
    @Transactional
    public TimelineView update(UpdateTimelineCommand command) {
        Timeline timeline = require(command.worldId(), command.timelineId());
        requireCalendar(command.worldId(), command.calendarId());
        timeline.update(command.name(), command.description(), command.calendarId(), clock.instant());
        return viewMapper.toView(timelines.save(timeline));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID timelineId) {
        timelines.delete(require(worldId, timelineId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineView> list(UUID worldId) {
        requireWorld(worldId);
        return timelines.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    // --- published lookup port ---

    @Override
    @Transactional(readOnly = true)
    public Optional<TimelineView> findById(UUID timelineId) {
        return timelines.findById(timelineId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineView> findByWorld(UUID worldId) {
        return timelines.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    private Timeline require(UUID worldId, UUID timelineId) {
        return timelines.findByIdAndWorld(timelineId, worldId)
                .orElseThrow(() -> new NotFoundException("Timeline not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireCalendar(UUID worldId, UUID calendarId) {
        if (calendarId != null && !calendars.existsInWorld(calendarId, worldId)) {
            throw new ValidationException("Calendar not found in this world");
        }
    }
}
