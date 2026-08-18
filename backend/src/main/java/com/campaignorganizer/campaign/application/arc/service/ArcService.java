package com.campaignorganizer.campaign.application.arc.service;

import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.CreateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.UpdateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.in.CreateArcUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.DeleteArcUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.ListArcsUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.UpdateArcUseCase;
import com.campaignorganizer.campaign.application.arc.port.out.ArcRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.domain.arc.Arc;
import com.campaignorganizer.campaign.domain.arc.ArcStatus;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arc use cases; also implements the published query port for consumers. */
@Service
public class ArcService implements CreateArcUseCase, UpdateArcUseCase, DeleteArcUseCase,
        ListArcsUseCase, ArcQueryPort {

    private final ArcRepositoryPort arcs;
    private final CampaignExistsPort campaigns;
    private final ArcViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public ArcService(ArcRepositoryPort arcs, CampaignExistsPort campaigns, ArcViewMapper viewMapper,
                      IdGenerator ids, Clock clock) {
        this.arcs = arcs;
        this.campaigns = campaigns;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ArcView create(CreateArcCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        ArcStatus status = command.status() == null ? ArcStatus.PLANNED : command.status();
        int position = command.position() == null ? 0 : command.position();
        Arc created = Arc.create(ids.newId(), command.campaignId(), command.title(),
                command.description(), status, position, clock.instant());
        return viewMapper.toView(arcs.save(created));
    }

    @Override
    @Transactional
    public ArcView update(UpdateArcCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        Arc arc = require(command.arcId(), command.campaignId());
        ArcStatus status = command.status() == null ? arc.getStatus() : command.status();
        int position = command.position() == null ? arc.getPosition() : command.position();
        arc.update(command.title(), command.description(), status, position, clock.instant());
        return viewMapper.toView(arcs.save(arc));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID arcId) {
        requireCampaign(worldId, campaignId);
        arcs.delete(require(arcId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArcView> list(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return findByCampaign(campaignId);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<ArcView> findByCampaign(UUID campaignId) {
        return arcs.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArcView> findById(UUID arcId) {
        return arcs.findById(arcId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInCampaign(UUID arcId, UUID campaignId) {
        return arcs.existsInCampaign(arcId, campaignId);
    }

    private Arc require(UUID arcId, UUID campaignId) {
        return arcs.findByIdAndCampaign(arcId, campaignId)
                .orElseThrow(() -> new NotFoundException("Arc not found"));
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }
}
