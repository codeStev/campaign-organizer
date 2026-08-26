package com.campaignorganizer.handouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.handouts.application.port.in.HandoutCommands.CreateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.UpdateHandoutCommand;
import com.campaignorganizer.handouts.application.port.out.HandoutRepositoryPort;
import com.campaignorganizer.handouts.application.port.out.WorldExistsPort;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.handouts.application.service.HandoutService;
import com.campaignorganizer.handouts.application.service.HandoutViewMapper;
import com.campaignorganizer.handouts.domain.Handout;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Handout use cases against mocked persistence; the style preset is the interesting rule. */
@ExtendWith(MockitoExtension.class)
class HandoutServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final Instant now = Instant.EPOCH;

    @Mock
    private HandoutRepositoryPort repo;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private HandoutService service;

    @BeforeEach
    void setUp() {
        lenient().when(worlds.exists(worldId)).thenReturn(true);
        lenient().when(ids.newId()).thenReturn(UUID.randomUUID());
        service = new HandoutService(repo, worlds, Mappers.getMapper(HandoutViewMapper.class),
                ids, Clock.fixed(now, java.time.ZoneOffset.UTC));
    }

    private Handout saved() {
        // Plain UUID here: calling the ids mock mid-stubbing trips UnfinishedStubbing.
        return Handout.create(UUID.randomUUID(), worldId, "Wanted poster", Handout.Preset.POSTER,
                "**500 gold**", now);
    }

    @Test
    void createRejectsUnknownWorld() {
        when(worlds.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> service.create(
                new CreateHandoutCommand(UUID.randomUUID(), "T", "POSTER", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsUnknownPreset() {
        assertThatThrownBy(() -> service.create(
                new CreateHandoutCommand(worldId, "T", "NEON", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("NEON");
    }

    @Test
    void createPersistsTheAggregate() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HandoutView view = service.create(new CreateHandoutCommand(worldId, "Letter",
                "LETTER", "Dear sir,"));

        assertThat(view.preset()).isEqualTo("LETTER");
        assertThat(view.body()).isEqualTo("Dear sir,");
        verify(repo).save(any(Handout.class));
    }

    @Test
    void updateAppliesToExistingHandoutOnly() {
        Handout existing = saved();
        when(repo.findByIdAndWorld(existing.getId(), worldId))
                .thenReturn(Optional.of(existing));

        service.update(new UpdateHandoutCommand(worldId, existing.getId(), "Newspaper piece",
                "NEWSPAPER", "Extra, extra"));

        assertThat(existing.getTitle()).isEqualTo("Newspaper piece");
        assertThat(existing.getPreset()).isEqualTo(Handout.Preset.NEWSPAPER);

        when(repo.findByIdAndWorld(existing.getId(), worldId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(new UpdateHandoutCommand(worldId,
                existing.getId(), "X", "POSTER", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listAndGetRequireTheWorldToExist() {
        when(repo.findByWorld(worldId)).thenReturn(List.of(saved()));

        assertThat(service.list(worldId)).hasSize(1);

        lenient().when(worlds.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> service.list(worldId)).isInstanceOf(NotFoundException.class);
    }
}
