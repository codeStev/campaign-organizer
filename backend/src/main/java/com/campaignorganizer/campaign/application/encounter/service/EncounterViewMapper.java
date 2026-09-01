package com.campaignorganizer.campaign.application.encounter.service;

import com.campaignorganizer.campaign.application.encounter.port.published.EncounterEntryView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import com.campaignorganizer.campaign.domain.encounter.Encounter;
import com.campaignorganizer.campaign.domain.encounter.EncounterEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EncounterViewMapper {

    EncounterView toView(Encounter encounter);

    EncounterEntryView toEntryView(EncounterEntry entry);
}
