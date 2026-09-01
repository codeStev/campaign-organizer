package com.campaignorganizer.campaign.application.campaign.service;

import com.campaignorganizer.campaign.application.campaign.port.in.GetCampaignRosterUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.RosterEntryInput;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.SetCampaignRosterCommand;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterEntry;
import com.campaignorganizer.campaign.application.campaign.port.in.SetCampaignRosterUseCase;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignPlayerRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.PlayerExistsPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign roster use cases (ADR-0091): a campaign's player membership,
 * each flagged regular or guest. Edited as a whole set — {@link #set} deletes
 * every existing row for the campaign and recreates it from the submitted
 * entries, the same shape as {@code TaggingService.set(...)}. Separate
 * service from {@link CampaignService}, the same way {@code CheatSheetService}
 * is separate from {@code SessionService} despite living in the same package.
 */
@Service
public class CampaignRosterService implements GetCampaignRosterUseCase, SetCampaignRosterUseCase,
        CampaignPlayerQueryPort, CampaignPlayerImportPort {

    private final CampaignPlayerRepositoryPort roster;
    private final CampaignRepositoryPort campaigns;
    private final PlayerExistsPort players;
    private final CampaignPlayerViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CampaignRosterService(CampaignPlayerRepositoryPort roster, CampaignRepositoryPort campaigns,
                                 PlayerExistsPort players, CampaignPlayerViewMapper viewMapper,
                                 IdGenerator ids, Clock clock) {
        this.roster = roster;
        this.campaigns = campaigns;
        this.players = players;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RosterEntry> get(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return toEntries(worldId, roster.findByCampaign(campaignId));
    }

    @Override
    @Transactional
    public List<RosterEntry> set(SetCampaignRosterCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        for (RosterEntryInput in : command.entries()) {
            if (!players.existsInWorld(in.playerId(), command.worldId())) {
                throw new ValidationException("Player not found in world: " + in.playerId());
            }
        }
        roster.deleteByCampaign(command.campaignId());
        // Built from what was just saved, not re-read: a query here would force
        // Hibernate to auto-flush, and it flushes pending inserts before pending
        // deletes, so a re-read would collide with the not-yet-deleted old rows.
        List<CampaignPlayer> saved = new ArrayList<>();
        for (RosterEntryInput in : command.entries()) {
            saved.add(roster.save(CampaignPlayer.create(ids.newId(), command.campaignId(),
                    in.playerId(), in.guest(), clock.instant())));
        }
        return toEntries(command.worldId(), saved);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public CampaignPlayerView importCampaignPlayer(CampaignPlayerView view) {
        CampaignPlayer entry = CampaignPlayer.reconstitute(view.id(), view.campaignId(),
                view.playerId(), view.guest(), view.createdAt());
        return viewMapper.toView(roster.save(entry));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<CampaignPlayerView> findByCampaign(UUID campaignId) {
        return roster.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    private List<RosterEntry> toEntries(UUID worldId, List<CampaignPlayer> rows) {
        return rows.stream()
                .map(r -> new RosterEntry(r.getPlayerId(),
                        players.findName(r.getPlayerId(), worldId).orElse("Unknown player"),
                        r.isGuest()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }
}
