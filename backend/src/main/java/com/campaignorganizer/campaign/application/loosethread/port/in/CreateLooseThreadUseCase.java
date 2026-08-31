package com.campaignorganizer.campaign.application.loosethread.port.in;

import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.CreateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;

public interface CreateLooseThreadUseCase {

    LooseThreadView create(CreateLooseThreadCommand command);
}
