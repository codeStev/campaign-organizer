package com.campaignorganizer.campaign.application.encounter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.CreateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.EntryInput;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.UpdateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.encounter.port.out.EncounterRepositoryPort;
import com.campaignorganizer.campaign.application.encounter.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterEntryView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import com.campaignorganizer.campaign.domain.encounter.Encounter;
import com.campaignorganizer.campaign.domain.encounter.EncounterEntry;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
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

/** Application service unit test for encounters (link validation) with mocked ports (ADR-0097). */
@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepositoryPort encounters;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private StatblockExistsPort statblocks;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final EncounterViewMapper viewMapper = new EncounterViewMapperImpl();

    private EncounterService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EncounterService(encounters, campaigns, statblocks, viewMapper, ids, clock);
    }

    private CreateEncounterCommand command(List<EntryInput> entries) {
        return new CreateEncounterCommand(worldId, campaignId, "Goblin ambush", "watch the road", entries);
    }

    @Test
    void createRejectsMissingCampaign() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignStatblock() {
        UUID statblockId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(statblocks.existsInWorld(statblockId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of(new EntryInput(statblockId, 1, null)))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createSucceedsWithValidEntries() {
        UUID statblockId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(statblocks.existsInWorld(statblockId, worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(encounters.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EncounterView view = service.create(command(List.of(new EntryInput(statblockId, 3, 12))));

        assertThat(view.name()).isEqualTo("Goblin ambush");
        assertThat(view.entries()).hasSize(1);
        assertThat(view.entries().get(0).statblockId()).isEqualTo(statblockId);
        assertThat(view.entries().get(0).quantity()).isEqualTo(3);
        assertThat(view.entries().get(0).maxHpOverride()).isEqualTo(12);
    }

    @Test
    void updateRejectsUnknownEncounter() {
        UUID encounterId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(encounters.findByIdAndCampaign(encounterId, campaignId)).thenReturn(Optional.empty());

        UpdateEncounterCommand cmd = new UpdateEncounterCommand(worldId, campaignId, encounterId, "Ambush",
                null, List.of());

        assertThatThrownBy(() -> service.update(cmd)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void existsInCampaignDelegatesToRepository() {
        UUID encounterId = UUID.randomUUID();
        Encounter found = Encounter.create(encounterId, campaignId, "Ambush", null, List.of(), clock.instant());
        when(encounters.findByIdAndCampaign(encounterId, campaignId))
                .thenReturn(Optional.of(found));

        assertThat(service.existsInCampaign(encounterId, campaignId)).isTrue();
    }

    @Test
    void existsInCampaignFalseWhenNotFound() {
        UUID encounterId = UUID.randomUUID();
        when(encounters.findByIdAndCampaign(encounterId, campaignId)).thenReturn(Optional.empty());

        assertThat(service.existsInCampaign(encounterId, campaignId)).isFalse();
    }

    @Test
    void importEncounterReconstitutesWithGivenIdAndEntries() {
        UUID encounterId = UUID.randomUUID();
        UUID statblockId = UUID.randomUUID();
        when(encounters.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EncounterView imported = new EncounterView(encounterId, campaignId, "Goblin ambush", "notes",
                List.of(new EncounterEntryView(statblockId, 2, null)), clock.instant(), clock.instant());
        EncounterView result = service.importEncounter(imported);

        assertThat(result.id()).isEqualTo(encounterId);
        assertThat(result.entries()).containsExactly(new EncounterEntryView(statblockId, 2, null));
    }
}
