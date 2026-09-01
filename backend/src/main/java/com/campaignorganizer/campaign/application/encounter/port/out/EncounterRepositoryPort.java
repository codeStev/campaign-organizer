package com.campaignorganizer.campaign.application.encounter.port.out;

import com.campaignorganizer.campaign.domain.encounter.Encounter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncounterRepositoryPort {

    List<Encounter> findByCampaign(UUID campaignId);

    Optional<Encounter> findByIdAndCampaign(UUID encounterId, UUID campaignId);

    Optional<Encounter> findById(UUID encounterId);

    Encounter save(Encounter encounter);

    void delete(Encounter encounter);
}
