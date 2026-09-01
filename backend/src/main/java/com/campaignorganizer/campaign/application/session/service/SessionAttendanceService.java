package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.AttendanceEntryInput;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.PutSessionAttendanceCommand;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceEntry;
import com.campaignorganizer.campaign.application.session.port.in.GetSessionAttendanceUseCase;
import com.campaignorganizer.campaign.application.session.port.in.PutSessionAttendanceUseCase;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.CharacterSheetExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.PlayerExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionAttendanceRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceView;
import com.campaignorganizer.campaign.domain.session.SessionAttendance;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session attendance use cases (ADR-0091): per session, per roster player,
 * present + optional character link. {@link #get} unions persisted rows
 * with the campaign's current roster, synthesizing a default (present, no
 * character) entry for any roster player without a saved row yet. {@link
 * #put} is a whole-set delete-then-recreate, mirroring {@link
 * CheatSheetService}'s upsert shape. Separate service from {@code
 * SessionService}, the same way {@link CheatSheetService} is.
 */
@Service
public class SessionAttendanceService implements GetSessionAttendanceUseCase, PutSessionAttendanceUseCase,
        SessionAttendanceQueryPort, SessionAttendanceImportPort {

    private final SessionAttendanceRepositoryPort attendance;
    private final SessionRepositoryPort sessions;
    private final CampaignExistsPort campaigns;
    private final CampaignPlayerQueryPort roster;
    private final PlayerExistsPort players;
    private final CharacterSheetExistsPort characterSheets;
    private final IdGenerator ids;
    private final Clock clock;

    public SessionAttendanceService(SessionAttendanceRepositoryPort attendance,
                                    SessionRepositoryPort sessions, CampaignExistsPort campaigns,
                                    CampaignPlayerQueryPort roster, PlayerExistsPort players,
                                    CharacterSheetExistsPort characterSheets, IdGenerator ids,
                                    Clock clock) {
        this.attendance = attendance;
        this.sessions = sessions;
        this.campaigns = campaigns;
        this.roster = roster;
        this.players = players;
        this.characterSheets = characterSheets;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceEntry> get(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        Map<UUID, SessionAttendance> saved = new HashMap<>();
        for (SessionAttendance row : attendance.findBySession(sessionId)) {
            saved.put(row.getPlayerId(), row);
        }
        return buildEntries(worldId, campaignId, saved);
    }

    @Override
    @Transactional
    public List<AttendanceEntry> put(PutSessionAttendanceCommand command) {
        requireSession(command.worldId(), command.campaignId(), command.sessionId());
        for (AttendanceEntryInput in : command.entries()) {
            if (!players.existsInWorld(in.playerId(), command.worldId())) {
                throw new ValidationException("Player not found in world: " + in.playerId());
            }
            if (in.characterId() != null && !characterSheets.existsForCampaign(in.characterId(),
                    command.worldId(), command.campaignId())) {
                throw new ValidationException(
                        "Character sheet not found for this campaign: " + in.characterId());
            }
        }
        attendance.deleteBySession(command.sessionId());
        // Built from what was just saved, not re-read: a query here would force
        // Hibernate to auto-flush, and it flushes pending inserts before pending
        // deletes, so a re-read would collide with the not-yet-deleted old rows.
        Map<UUID, SessionAttendance> saved = new HashMap<>();
        for (AttendanceEntryInput in : command.entries()) {
            SessionAttendance row = attendance.save(SessionAttendance.create(ids.newId(),
                    command.sessionId(), in.playerId(), in.present(), in.characterId(), clock.instant()));
            saved.put(row.getPlayerId(), row);
        }
        return buildEntries(command.worldId(), command.campaignId(), saved);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public SessionAttendanceView importAttendance(SessionAttendanceView view) {
        SessionAttendance row = SessionAttendance.reconstitute(view.id(), view.sessionId(),
                view.playerId(), view.present(), view.characterId(), view.createdAt());
        return toView(attendance.save(row));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<SessionAttendanceView> findBySession(UUID sessionId) {
        return attendance.findBySession(sessionId).stream().map(this::toView).toList();
    }

    /**
     * Unions the campaign roster with any saved attendance rows for the session.
     * A roster player with no saved row yet defaults to present with no
     * character. A saved row for a player no longer on the roster still
     * appears (historical accuracy, ADR-0091) — it's just missing the guest
     * flag the roster would have supplied, so that defaults to {@code false}.
     */
    private List<AttendanceEntry> buildEntries(UUID worldId, UUID campaignId,
                                               Map<UUID, SessionAttendance> saved) {
        Map<UUID, Boolean> guestByPlayer = new HashMap<>();
        for (CampaignPlayerView r : roster.findByCampaign(campaignId)) {
            guestByPlayer.put(r.playerId(), r.guest());
        }
        Set<UUID> playerIds = new LinkedHashSet<>(guestByPlayer.keySet());
        playerIds.addAll(saved.keySet());

        List<AttendanceEntry> out = new ArrayList<>();
        for (UUID playerId : playerIds) {
            SessionAttendance row = saved.get(playerId);
            boolean present = row == null || row.isPresent();
            UUID characterId = row == null ? null : row.getCharacterId();
            boolean guest = guestByPlayer.getOrDefault(playerId, false);
            String name = players.findName(playerId, worldId).orElse("Unknown player");
            String characterName = characterId == null ? null
                    : characterSheets.findName(characterId, worldId).orElse(null);
            out.add(new AttendanceEntry(playerId, name, guest, present, characterId, characterName));
        }
        return out.stream().sorted(Comparator.comparing(AttendanceEntry::name,
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private SessionAttendanceView toView(SessionAttendance a) {
        return new SessionAttendanceView(a.getId(), a.getSessionId(), a.getPlayerId(), a.isPresent(),
                a.getCharacterId(), a.getCreatedAt());
    }

    private void requireSession(UUID worldId, UUID campaignId, UUID sessionId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
        sessions.findByIdAndCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }
}
