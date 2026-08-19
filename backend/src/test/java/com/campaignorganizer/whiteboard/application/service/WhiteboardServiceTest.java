package com.campaignorganizer.whiteboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.CreateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.out.WhiteboardRepositoryPort;
import com.campaignorganizer.whiteboard.application.port.out.WorldExistsPort;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test with mocked out-ports — no Spring context, no DB. */
@ExtendWith(MockitoExtension.class)
class WhiteboardServiceTest {

    @Mock
    private WhiteboardRepositoryPort whiteboards;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    // The generated MapStruct mapper is a plain object — instantiate it directly.
    private final WhiteboardViewMapper viewMapper = new WhiteboardViewMapperImpl();

    private WhiteboardService service;

    @BeforeEach
    void setUp() {
        service = new WhiteboardService(whiteboards, worlds, viewMapper, ids, clock);
    }

    @Test
    void createGeneratesIdStampsTimeAndReturnsView() {
        UUID worldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(newId);
        when(whiteboards.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WhiteboardView view = service.create(
                new CreateWhiteboardCommand(worldId, "Plot", List.of(), List.of()));

        assertThat(view.id()).isEqualTo(newId);
        assertThat(view.name()).isEqualTo("Plot");
        assertThat(view.createdAt()).isEqualTo(Instant.parse("2026-03-03T00:00:00Z"));
    }

    @Test
    void createRejectsUnknownWorld() {
        when(worlds.exists(any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateWhiteboardCommand(UUID.randomUUID(), "x", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMissingWhiteboardThrows() {
        when(whiteboards.findByIdAndWorld(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
