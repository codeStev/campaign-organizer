package com.campaignorganizer.campaign.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.AttendanceEntryInput;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.PutSessionAttendanceCommand;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceEntry;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.CharacterSheetExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.PlayerExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionAttendanceRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.domain.session.Session;
import com.campaignorganizer.campaign.domain.session.SessionAttendance;
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

/** Application service unit test for session attendance (ADR-0091) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class SessionAttendanceServiceTest {

    @Mock
    private SessionAttendanceRepositoryPort attendance;
    @Mock
    private SessionRepositoryPort sessions;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private CampaignPlayerQueryPort roster;
    @Mock
    private PlayerExistsPort players;
    @Mock
    private CharacterSheetExistsPort characterSheets;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-05T00:00:00Z"), ZoneOffset.UTC);

    private SessionAttendanceService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SessionAttendanceService(attendance, sessions, campaigns, roster, players,
                characterSheets, ids, clock);
    }

    private void sessionExists() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(sessions.findByIdAndCampaign(sessionId, campaignId))
                .thenReturn(Optional.of(Session.create(UUID.randomUUID(), campaignId, "S1", 1,
                        null, null, null, clock.instant())));
    }

    @Test
    void getRequiresTheSessionToExist() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.get(worldId, campaignId, sessionId))
                .isInstanceOf(NotFoundException.class);
        verify(roster, never()).findByCampaign(any());
    }

    @Test
    void getDefaultsARosterPlayerWithNoSavedRowToPresent() {
        sessionExists();
        when(roster.findByCampaign(campaignId)).thenReturn(
                List.of(new CampaignPlayerView(UUID.randomUUID(), campaignId, playerId, false,
                        clock.instant())));
        when(attendance.findBySession(sessionId)).thenReturn(List.of());
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));

        List<AttendanceEntry> entries = service.get(worldId, campaignId, sessionId);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).present()).isTrue();
        assertThat(entries.get(0).characterId()).isNull();
        assertThat(entries.get(0).name()).isEqualTo("Dana");
    }

    @Test
    void getUsesTheSavedRowWhenOneExists() {
        sessionExists();
        when(roster.findByCampaign(campaignId)).thenReturn(
                List.of(new CampaignPlayerView(UUID.randomUUID(), campaignId, playerId, false,
                        clock.instant())));
        when(attendance.findBySession(sessionId)).thenReturn(List.of(
                SessionAttendance.create(UUID.randomUUID(), sessionId, playerId, false, characterId,
                        clock.instant())));
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));
        when(characterSheets.findName(characterId, worldId)).thenReturn(Optional.of("Kessa"));

        List<AttendanceEntry> entries = service.get(worldId, campaignId, sessionId);

        assertThat(entries.get(0).present()).isFalse();
        assertThat(entries.get(0).characterId()).isEqualTo(characterId);
        assertThat(entries.get(0).characterName()).isEqualTo("Kessa");
    }

    @Test
    void putReplacesTheWholeSet() {
        sessionExists();
        when(players.existsInWorld(playerId, worldId)).thenReturn(true);
        when(characterSheets.existsForCampaign(characterId, worldId, campaignId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(attendance.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roster.findByCampaign(campaignId)).thenReturn(
                List.of(new CampaignPlayerView(UUID.randomUUID(), campaignId, playerId, false,
                        clock.instant())));
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));
        when(characterSheets.findName(characterId, worldId)).thenReturn(Optional.of("Kessa"));

        List<AttendanceEntry> entries = service.put(new PutSessionAttendanceCommand(worldId, campaignId,
                sessionId, List.of(new AttendanceEntryInput(playerId, true, characterId))));

        verify(attendance).deleteBySession(sessionId);
        verify(attendance).save(any());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).present()).isTrue();
    }

    @Test
    void putRejectsAnUnknownPlayer() {
        sessionExists();
        when(players.existsInWorld(playerId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.put(new PutSessionAttendanceCommand(worldId, campaignId,
                sessionId, List.of(new AttendanceEntryInput(playerId, true, null)))))
                .isInstanceOf(ValidationException.class);
        verify(attendance, never()).deleteBySession(any());
    }

    @Test
    void putRejectsACharacterSheetFromAnotherCampaign() {
        sessionExists();
        when(players.existsInWorld(playerId, worldId)).thenReturn(true);
        when(characterSheets.existsForCampaign(characterId, worldId, campaignId)).thenReturn(false);

        assertThatThrownBy(() -> service.put(new PutSessionAttendanceCommand(worldId, campaignId,
                sessionId, List.of(new AttendanceEntryInput(playerId, true, characterId)))))
                .isInstanceOf(ValidationException.class);
        verify(attendance, never()).deleteBySession(any());
    }

    @Test
    void attendanceSurvivesRosterRemoval() {
        sessionExists();
        // Player has a saved attendance row but is no longer on the roster.
        when(roster.findByCampaign(campaignId)).thenReturn(List.of());
        when(attendance.findBySession(sessionId)).thenReturn(List.of(
                SessionAttendance.create(UUID.randomUUID(), sessionId, playerId, true, null,
                        clock.instant())));
        when(players.findName(playerId, worldId)).thenReturn(Optional.of("Dana"));

        List<AttendanceEntry> entries = service.get(worldId, campaignId, sessionId);

        // The row still surfaces even though the roster no longer lists this player —
        // a roster edit must never silently erase attendance history.
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).playerId()).isEqualTo(playerId);
        assertThat(entries.get(0).present()).isTrue();
        assertThat(entries.get(0).guest()).isFalse();
        verify(attendance, never()).deleteBySession(any());
    }
}
