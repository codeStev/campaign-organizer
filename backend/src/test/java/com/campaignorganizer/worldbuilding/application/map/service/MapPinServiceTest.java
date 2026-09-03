package com.campaignorganizer.worldbuilding.application.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.CreateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.map.port.out.MapPinRepositoryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for map pins with mocked ports. */
@ExtendWith(MockitoExtension.class)
class MapPinServiceTest {

    @Mock
    private MapPinRepositoryPort pins;
    @Mock
    private MapQueryPort maps;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final MapPinViewMapper viewMapper = new MapPinViewMapperImpl();

    private MapPinService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID mapId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MapPinService(pins, maps, articles, viewMapper, ids, clock);
    }

    private MapView map() {
        return new MapView(mapId, worldId, null, "Overworld", UUID.randomUUID(), Instant.now(), Instant.now());
    }

    @Test
    void createWithoutArticleSucceeds() {
        when(maps.findByIdInWorld(mapId, worldId)).thenReturn(Optional.of(map()));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pins.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MapPinView view = service.create(new CreateMapPinCommand(
                worldId, mapId, null, "start", null, 0.5, 0.5));

        assertThat(view.label()).isEqualTo("start");
        assertThat(view.x()).isEqualTo(0.5);
    }

    @Test
    void createRejectsMissingMap() {
        when(maps.findByIdInWorld(mapId, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateMapPinCommand(
                worldId, mapId, null, "x", null, 0.1, 0.1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignArticle() {
        when(maps.findByIdInWorld(mapId, worldId)).thenReturn(Optional.of(map()));
        when(articles.existsInWorld(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateMapPinCommand(
                worldId, mapId, UUID.randomUUID(), "x", null, 0.1, 0.1)))
                .isInstanceOf(ValidationException.class);
    }
}
