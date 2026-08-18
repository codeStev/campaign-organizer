package com.campaignorganizer.campaign.application.arc.service;

import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.CreateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.UpdateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.in.CreateBeatUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.DeleteBeatUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.ListBeatsUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.UpdateBeatUseCase;
import com.campaignorganizer.campaign.application.arc.port.out.ArcBeatRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.out.ArcRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.out.ArticleExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.arc.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.domain.arc.ArcBeat;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Beat write use cases (create/update/delete) plus the per-arc listing, with link validation. */
@Service
public class ArcBeatCommandService implements CreateBeatUseCase, UpdateBeatUseCase, DeleteBeatUseCase,
        ListBeatsUseCase {

    private final ArcBeatRepositoryPort beats;
    private final ArcRepositoryPort arcs;
    private final CampaignExistsPort campaigns;
    private final ArticleExistsPort articles;
    private final StatblockExistsPort statblocks;
    private final SessionExistsPort sessions;
    private final ArcBeatViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public ArcBeatCommandService(ArcBeatRepositoryPort beats, ArcRepositoryPort arcs,
                                 CampaignExistsPort campaigns, ArticleExistsPort articles,
                                 StatblockExistsPort statblocks, SessionExistsPort sessions,
                                 ArcBeatViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.beats = beats;
        this.arcs = arcs;
        this.campaigns = campaigns;
        this.articles = articles;
        this.statblocks = statblocks;
        this.sessions = sessions;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ArcBeatView create(CreateBeatCommand command) {
        requireArc(command.worldId(), command.campaignId(), command.arcId());
        validateLinks(command.worldId(), command.campaignId(), command.articleIds(),
                command.statblockIds(), command.sessionId());
        int position = command.position() == null ? 0 : command.position();
        ArcBeat created = ArcBeat.create(ids.newId(), command.arcId(), command.articleIds(),
                command.statblockIds(), command.sessionId(), command.title(), command.body(),
                command.done(), position, clock.instant());
        return viewMapper.toView(beats.save(created));
    }

    @Override
    @Transactional
    public ArcBeatView update(UpdateBeatCommand command) {
        requireArc(command.worldId(), command.campaignId(), command.arcId());
        validateLinks(command.worldId(), command.campaignId(), command.articleIds(),
                command.statblockIds(), command.sessionId());
        ArcBeat beat = beats.findByIdAndArc(command.beatId(), command.arcId())
                .orElseThrow(() -> new NotFoundException("Beat not found"));
        int position = command.position() == null ? beat.getPosition() : command.position();
        beat.update(command.articleIds(), command.statblockIds(), command.sessionId(), command.title(),
                command.body(), command.done(), position, clock.instant());
        return viewMapper.toView(beats.save(beat));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID arcId, UUID beatId) {
        requireArc(worldId, campaignId, arcId);
        ArcBeat beat = beats.findByIdAndArc(beatId, arcId)
                .orElseThrow(() -> new NotFoundException("Beat not found"));
        beats.delete(beat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArcBeatView> list(UUID worldId, UUID campaignId, UUID arcId) {
        requireArc(worldId, campaignId, arcId);
        return beats.findByArc(arcId).stream().map(viewMapper::toView).toList();
    }

    private void requireArc(UUID worldId, UUID campaignId, UUID arcId) {
        if (!campaigns.existsInWorld(campaignId, worldId) || !arcs.existsInCampaign(arcId, campaignId)) {
            throw new NotFoundException("Arc not found");
        }
    }

    private void validateLinks(UUID worldId, UUID campaignId, List<UUID> articleIds,
                               List<UUID> statblockIds, UUID sessionId) {
        for (UUID articleId : articleIds) {
            if (!articles.existsInWorld(articleId, worldId)) {
                throw new ValidationException("Article not found in this world");
            }
        }
        for (UUID statblockId : statblockIds) {
            if (!statblocks.existsInWorld(statblockId, worldId)) {
                throw new ValidationException("Statblock not found in this world");
            }
        }
        if (sessionId != null && !sessions.existsInCampaign(sessionId, campaignId)) {
            throw new ValidationException("Session not found in this campaign");
        }
    }
}
