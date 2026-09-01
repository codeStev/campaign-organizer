package com.campaignorganizer.campaign.application.encounter.service;

import com.campaignorganizer.campaign.application.encounter.port.in.CreateEncounterUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.DeleteEncounterUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.CreateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.EntryInput;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.UpdateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.in.ListEncountersUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.UpdateEncounterUseCase;
import com.campaignorganizer.campaign.application.encounter.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.encounter.port.out.EncounterRepositoryPort;
import com.campaignorganizer.campaign.application.encounter.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterEntryView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterImportPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterQueryPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import com.campaignorganizer.campaign.domain.encounter.Encounter;
import com.campaignorganizer.campaign.domain.encounter.EncounterEntry;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Encounter use cases (ADR-0097); also implements the published query/import ports for consumers. */
@Service
public class EncounterService implements CreateEncounterUseCase, UpdateEncounterUseCase,
        DeleteEncounterUseCase, ListEncountersUseCase, EncounterQueryPort, EncounterImportPort {

    private final EncounterRepositoryPort encounters;
    private final CampaignExistsPort campaigns;
    private final StatblockExistsPort statblocks;
    private final EncounterViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public EncounterService(EncounterRepositoryPort encounters, CampaignExistsPort campaigns,
                            StatblockExistsPort statblocks, EncounterViewMapper viewMapper, IdGenerator ids,
                            Clock clock) {
        this.encounters = encounters;
        this.campaigns = campaigns;
        this.statblocks = statblocks;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EncounterView create(CreateEncounterCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        validateEntries(command.worldId(), command.entries());
        Encounter created = Encounter.create(ids.newId(), command.campaignId(), command.name(),
                command.notes(), toDomainEntries(command.entries()), clock.instant());
        return viewMapper.toView(encounters.save(created));
    }

    @Override
    @Transactional
    public EncounterView update(UpdateEncounterCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        validateEntries(command.worldId(), command.entries());
        Encounter encounter = require(command.encounterId(), command.campaignId());
        encounter.update(command.name(), command.notes(), toDomainEntries(command.entries()), clock.instant());
        return viewMapper.toView(encounters.save(encounter));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID encounterId) {
        requireCampaign(worldId, campaignId);
        encounters.delete(require(encounterId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EncounterView> list(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return findByCampaign(campaignId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public EncounterView importEncounter(EncounterView view) {
        Encounter encounter = Encounter.reconstitute(view.id(), view.campaignId(), view.name(), view.notes(),
                toDomainEntriesFromViews(view.entries()), view.createdAt(), view.updatedAt());
        return viewMapper.toView(encounters.save(encounter));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<EncounterView> findByCampaign(UUID campaignId) {
        return encounters.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EncounterView> findById(UUID encounterId) {
        return encounters.findById(encounterId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInCampaign(UUID encounterId, UUID campaignId) {
        return encounters.findByIdAndCampaign(encounterId, campaignId).isPresent();
    }

    private void validateEntries(UUID worldId, List<EntryInput> entries) {
        if (entries == null) {
            return;
        }
        for (EntryInput entry : entries) {
            if (!statblocks.existsInWorld(entry.statblockId(), worldId)) {
                throw new ValidationException("Statblock not found in this world");
            }
        }
    }

    private static List<EncounterEntry> toDomainEntries(List<EntryInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(e -> new EncounterEntry(e.statblockId(), e.quantity(), e.maxHpOverride()))
                .toList();
    }

    private static List<EncounterEntry> toDomainEntriesFromViews(List<EncounterEntryView> views) {
        if (views == null) {
            return List.of();
        }
        return views.stream()
                .map(e -> new EncounterEntry(e.statblockId(), e.quantity(), e.maxHpOverride()))
                .toList();
    }

    private Encounter require(UUID encounterId, UUID campaignId) {
        return encounters.findByIdAndCampaign(encounterId, campaignId)
                .orElseThrow(() -> new NotFoundException("Encounter not found"));
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }
}
