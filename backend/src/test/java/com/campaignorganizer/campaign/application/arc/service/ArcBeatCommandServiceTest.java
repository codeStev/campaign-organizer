package com.campaignorganizer.campaign.application.arc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.CreateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.out.ArcBeatRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.out.ArcRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.out.ArticleExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.DeckExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.TableExistsPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterQueryPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
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

/** Application service unit test for beat writes (link validation) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class ArcBeatCommandServiceTest {

    @Mock
    private ArcBeatRepositoryPort beats;
    @Mock
    private ArcRepositoryPort arcs;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private StatblockExistsPort statblocks;
    @Mock
    private TableExistsPort tables;
    @Mock
    private DeckExistsPort decks;
    @Mock
    private SessionExistsPort sessions;
    @Mock
    private EncounterQueryPort encounters;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final ArcBeatViewMapper viewMapper = new ArcBeatViewMapper() {
    };

    private ArcBeatCommandService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID arcId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ArcBeatCommandService(beats, arcs, campaigns, articles, statblocks, tables,
                decks, sessions, encounters, viewMapper, ids, clock);
    }

    private CreateBeatCommand command(List<UUID> articleIds, List<UUID> statblockIds, UUID sessionId) {
        return new CreateBeatCommand(worldId, campaignId, arcId, "Ambush", "body", false,
                articleIds, statblockIds, List.of(), List.of(), List.of(), sessionId, 0);
    }

    private CreateBeatCommand commandWithEncounters(List<UUID> encounterIds) {
        return new CreateBeatCommand(worldId, campaignId, arcId, "Ambush", "body", false,
                List.of(), List.of(), encounterIds, List.of(), List.of(), null, 0);
    }

    @Test
    void createSucceedsWithValidLinks() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(arcs.existsInCampaign(arcId, campaignId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(beats.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcBeatView view = service.create(command(List.of(), List.of(), null));

        assertThat(view.title()).isEqualTo("Ambush");
    }

    @Test
    void createRejectsMissingArc() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(arcs.existsInCampaign(arcId, campaignId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of(), List.of(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignStatblock() {
        UUID statblockId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(arcs.existsInCampaign(arcId, campaignId)).thenReturn(true);
        when(statblocks.existsInWorld(statblockId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of(), List.of(statblockId), null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsForeignEncounter() {
        UUID encounterId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(arcs.existsInCampaign(arcId, campaignId)).thenReturn(true);
        when(encounters.existsInCampaign(encounterId, campaignId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(commandWithEncounters(List.of(encounterId))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createSucceedsWithValidEncounter() {
        UUID encounterId = UUID.randomUUID();
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(arcs.existsInCampaign(arcId, campaignId)).thenReturn(true);
        when(encounters.existsInCampaign(encounterId, campaignId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(beats.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcBeatView view = service.create(commandWithEncounters(List.of(encounterId)));

        assertThat(view.encounterIds()).containsExactly(encounterId);
    }
}
