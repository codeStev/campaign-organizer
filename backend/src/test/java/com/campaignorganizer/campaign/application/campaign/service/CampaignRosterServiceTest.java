package com.campaignorganizer.campaign.application.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.RosterEntryInput;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.SetCampaignRosterCommand;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterEntry;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignPlayerRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.PlayerExistsPort;
import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
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

/** Application service unit test for the campaign roster (ADR-0091) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class CampaignRosterServiceTest {

    @Mock
    private CampaignPlayerRepositoryPort roster;
    @Mock
    private CampaignRepositoryPort campaigns;
    @Mock
    private PlayerExistsPort players;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-05T00:00:00Z"), ZoneOffset.UTC);
    private final CampaignPlayerViewMapper viewMapper = new CampaignPlayerViewMapperImpl();

    private CampaignRosterService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignRosterService(roster, campaigns, players, viewMapper, ids, clock);
    }

    private void campaignExists() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
    }

    @Test
    void getRequiresTheCampaignToExist() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.get(worldId, campaignId)).isInstanceOf(NotFoundException.class);
        verify(roster, never()).findByCampaign(any());
    }

    @Test
    void getReturnsDenormalizedRosterEntries() {
        campaignExists();
        CampaignPlayer row = CampaignPlayer.create(UUID.randomUUID(), campaignId, playerId, true,
                clock.instant());
        when(roster.findByCampaign(campaignId)).thenReturn(List.of(row));
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));

        List<RosterEntry> entries = service.get(worldId, campaignId);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).playerId()).isEqualTo(playerId);
        assertThat(entries.get(0).name()).isEqualTo("Dana");
        assertThat(entries.get(0).guest()).isTrue();
    }

    @Test
    void setReplacesTheWholeRoster() {
        campaignExists();
        when(players.existsInWorld(playerId, worldId)).thenReturn(true);
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        UUID savedRowId = UUID.randomUUID();
        when(roster.save(any())).thenAnswer(inv -> {
            CampaignPlayer input = inv.getArgument(0);
            return CampaignPlayer.reconstitute(savedRowId, input.getCampaignId(), input.getPlayerId(),
                    input.isGuest(), input.getCreatedAt());
        });

        List<RosterEntry> entries = service.set(new SetCampaignRosterCommand(worldId, campaignId,
                List.of(new RosterEntryInput(playerId, false))));

        verify(roster).deleteByCampaign(campaignId);
        verify(roster).save(any());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).guest()).isFalse();
    }

    @Test
    void setRejectsAnUnknownPlayer() {
        campaignExists();
        when(players.existsInWorld(playerId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.set(new SetCampaignRosterCommand(worldId, campaignId,
                List.of(new RosterEntryInput(playerId, false)))))
                .isInstanceOf(ValidationException.class);
        verify(roster, never()).deleteByCampaign(any());
    }
}
