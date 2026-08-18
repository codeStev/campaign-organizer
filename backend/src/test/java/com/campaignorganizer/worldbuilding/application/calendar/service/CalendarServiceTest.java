package com.campaignorganizer.worldbuilding.application.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.CreateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.MonthInput;
import com.campaignorganizer.worldbuilding.application.calendar.port.out.CalendarRepositoryPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test with mocked out-ports — no Spring, no DB. */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarRepositoryPort calendars;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final CalendarViewMapper viewMapper = new CalendarViewMapperImpl();

    private CalendarService service;

    @BeforeEach
    void setUp() {
        service = new CalendarService(calendars, worlds, viewMapper, ids, clock);
    }

    @Test
    void createBuildsAggregateWithMonthsAndReturnsView() {
        UUID worldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(newId);
        when(calendars.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CalendarView view = service.create(new CreateCalendarCommand(worldId, "Reckoning", 7,
                List.of(new MonthInput("Frostfall", 30), new MonthInput("Sunreach", 31))));

        assertThat(view.id()).isEqualTo(newId);
        assertThat(view.months()).hasSize(2);
        assertThat(view.months().get(1).days()).isEqualTo(31);
    }

    @Test
    void createRejectsUnknownWorld() {
        when(worlds.exists(any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateCalendarCommand(UUID.randomUUID(), "R", 7, List.of(new MonthInput("A", 1)))))
                .isInstanceOf(NotFoundException.class);
    }
}
