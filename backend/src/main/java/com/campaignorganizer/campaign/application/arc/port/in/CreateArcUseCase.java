package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.CreateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;

public interface CreateArcUseCase {

    ArcView create(CreateArcCommand command);
}
