package com.campaignorganizer.worldbuilding.application.world.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.CreateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.out.WorldRepositoryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import com.campaignorganizer.worldbuilding.domain.world.World;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for worlds with mocked ports. */
@ExtendWith(MockitoExtension.class)
class WorldServiceTest {

    @Mock
    private WorldRepositoryPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final WorldViewMapper viewMapper = new WorldViewMapperImpl();

    private WorldService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorldService(worlds, viewMapper, ids, clock);
    }

    @Test
    void createReturnsViewWithGeneratedId() {
        when(ids.newId()).thenReturn(worldId);
        when(worlds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorldView view = service.create(new CreateWorldCommand("Aetheria", "desc"));

        assertThat(view.id()).isEqualTo(worldId);
        assertThat(view.name()).isEqualTo("Aetheria");
    }

    @Test
    void getRejectsMissingWorld() {
        when(worlds.findById(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(worldId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void replaceLayerStylesPersistsAndReturns() {
        World world = World.create(worldId, "Aetheria", null, clock.instant());
        when(worlds.findById(worldId)).thenReturn(Optional.of(world));
        when(worlds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, LayerStyle> result = service.replace(worldId,
                Map.of("towns", new LayerStyle("#ff0000", "castle")));

        assertThat(result).containsKey("towns");
    }
}
