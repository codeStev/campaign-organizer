package com.campaignorganizer.campaign.application.beatkind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.CreateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.UpdateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.out.BeatKindRepositoryPort;
import com.campaignorganizer.campaign.application.beatkind.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
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

/** Application service unit test for beat kinds (ADR-0101) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class BeatKindServiceTest {

    @Mock
    private BeatKindRepositoryPort beatKinds;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final BeatKindViewMapper viewMapper = new BeatKindViewMapperImpl();

    private BeatKindService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BeatKindService(beatKinds, worlds, viewMapper, ids, clock);
    }

    @Test
    void createReturnsViewWithGeneratedId() {
        UUID id = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(id);
        when(beatKinds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BeatKindView view = service.create(new CreateBeatKindCommand(worldId, "Combat", "#c0392b"));

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.name()).isEqualTo("Combat");
        assertThat(view.color()).isEqualTo("#c0392b");
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateBeatKindCommand(worldId, "Combat", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsDuplicateNameInSameWorld() {
        BeatKind existing = BeatKind.create(UUID.randomUUID(), worldId, "Combat", null, clock.instant());
        when(worlds.exists(worldId)).thenReturn(true);
        when(beatKinds.findByNameIgnoreCaseAndWorld("Combat", worldId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(new CreateBeatKindCommand(worldId, "Combat", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateRenamesAnExistingBeatKind() {
        UUID id = UUID.randomUUID();
        BeatKind existing = BeatKind.create(id, worldId, "Combat", null, Instant.parse("2026-03-01T00:00:00Z"));
        when(beatKinds.findByIdAndWorld(id, worldId)).thenReturn(Optional.of(existing));
        when(beatKinds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BeatKindView view = service.update(new UpdateBeatKindCommand(worldId, id, "Skirmish", "#ff0000"));

        assertThat(view.name()).isEqualTo("Skirmish");
        assertThat(view.color()).isEqualTo("#ff0000");
        assertThat(view.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void updateAllowsKeepingItsOwnName() {
        UUID id = UUID.randomUUID();
        BeatKind existing = BeatKind.create(id, worldId, "Combat", null, clock.instant());
        when(beatKinds.findByIdAndWorld(id, worldId)).thenReturn(Optional.of(existing));
        when(beatKinds.findByNameIgnoreCaseAndWorld("Combat", worldId)).thenReturn(Optional.of(existing));
        when(beatKinds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BeatKindView view = service.update(new UpdateBeatKindCommand(worldId, id, "Combat", "#ff0000"));

        assertThat(view.color()).isEqualTo("#ff0000");
    }

    @Test
    void deleteRemovesTheExistingBeatKind() {
        UUID id = UUID.randomUUID();
        BeatKind existing = BeatKind.create(id, worldId, "Combat", null, clock.instant());
        when(beatKinds.findByIdAndWorld(id, worldId)).thenReturn(Optional.of(existing));

        service.delete(worldId, id);

        verify(beatKinds).delete(existing);
    }

    @Test
    void getRejectsAnUnknownBeatKind() {
        UUID id = UUID.randomUUID();
        when(beatKinds.findByIdAndWorld(id, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(worldId, id)).isInstanceOf(NotFoundException.class);
    }
}
