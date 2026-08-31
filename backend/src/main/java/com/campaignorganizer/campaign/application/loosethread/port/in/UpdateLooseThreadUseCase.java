package com.campaignorganizer.campaign.application.loosethread.port.in;

import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.UpdateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;

public interface UpdateLooseThreadUseCase {

    LooseThreadView update(UpdateLooseThreadCommand command);
}
