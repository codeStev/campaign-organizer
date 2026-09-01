package com.campaignorganizer.campaign.application.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.CreatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.UpdatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.out.PlayerRepositoryPort;
import com.campaignorganizer.campaign.application.player.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import com.campaignorganizer.campaign.domain.player.Player;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
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

/** Application service unit test for players (ADR-0091) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepositoryPort players;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final PlayerViewMapper viewMapper = new PlayerViewMapperImpl();

    private PlayerService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PlayerService(players, worlds, viewMapper, ids, clock);
    }

    @Test
    void createReturnsViewWithGeneratedId() {
        UUID id = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(id);
        when(players.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlayerView view = service.create(new CreatePlayerCommand(worldId, "Dana"));

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.name()).isEqualTo("Dana");
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreatePlayerCommand(worldId, "Dana")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRenamesAnExistingPlayer() {
        UUID id = UUID.randomUUID();
        Player existing = Player.create(id, worldId, "Dana", Instant.parse("2026-03-01T00:00:00Z"));
        when(players.findByIdAndWorld(id, worldId)).thenReturn(Optional.of(existing));
        when(players.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlayerView view = service.update(new UpdatePlayerCommand(worldId, id, "Dana Reyes"));

        assertThat(view.name()).isEqualTo("Dana Reyes");
        assertThat(view.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void deleteRemovesTheExistingPlayer() {
        UUID id = UUID.randomUUID();
        Player existing = Player.create(id, worldId, "Dana", clock.instant());
        when(players.findByIdAndWorld(id, worldId)).thenReturn(Optional.of(existing));

        service.delete(worldId, id);

        verify(players).delete(existing);
    }

    @Test
    void getRejectsAnUnknownPlayer() {
        UUID id = UUID.randomUUID();
        when(players.findByIdAndWorld(id, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(worldId, id)).isInstanceOf(NotFoundException.class);
    }
}
