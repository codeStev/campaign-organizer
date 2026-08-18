package com.campaignorganizer.campaign.application.campaign.service;

import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.CreateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.UpdateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.in.CreateCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.DeleteCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.GetCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.ListCampaignsUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.UpdateCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.Campaign;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Campaign use cases; also implements the published query port for consumers. */
@Service
public class CampaignService implements CreateCampaignUseCase, UpdateCampaignUseCase,
        DeleteCampaignUseCase, GetCampaignUseCase, ListCampaignsUseCase, CampaignQueryPort {

    private final CampaignRepositoryPort campaigns;
    private final WorldExistsPort worlds;
    private final CampaignViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CampaignService(CampaignRepositoryPort campaigns, WorldExistsPort worlds,
                           CampaignViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.campaigns = campaigns;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CampaignView create(CreateCampaignCommand command) {
        requireWorld(command.worldId());
        Campaign created = Campaign.create(ids.newId(), command.worldId(), command.name(),
                command.description(), command.notes(), clock.instant());
        return viewMapper.toView(campaigns.save(created));
    }

    @Override
    @Transactional
    public CampaignView update(UpdateCampaignCommand command) {
        Campaign campaign = require(command.worldId(), command.campaignId());
        campaign.update(command.name(), command.description(), command.notes(), clock.instant());
        return viewMapper.toView(campaigns.save(campaign));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId) {
        campaigns.delete(require(worldId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignView get(UUID worldId, UUID campaignId) {
        return viewMapper.toView(require(worldId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<CampaignView> findByWorld(UUID worldId) {
        return campaigns.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignView> findByIdInWorld(UUID campaignId, UUID worldId) {
        return campaigns.findByIdAndWorld(campaignId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignView> findById(UUID campaignId) {
        return campaigns.findById(campaignId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }

    private Campaign require(UUID worldId, UUID campaignId) {
        return campaigns.findByIdAndWorld(campaignId, worldId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
