package com.campaignorganizer.campaign.application.encounter.port.in;

import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.UpdateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;

public interface UpdateEncounterUseCase {

    EncounterView update(UpdateEncounterCommand command);
}
