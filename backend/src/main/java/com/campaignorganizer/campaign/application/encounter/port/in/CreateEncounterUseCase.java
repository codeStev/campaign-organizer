package com.campaignorganizer.campaign.application.encounter.port.in;

import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.CreateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;

public interface CreateEncounterUseCase {

    EncounterView create(CreateEncounterCommand command);
}
