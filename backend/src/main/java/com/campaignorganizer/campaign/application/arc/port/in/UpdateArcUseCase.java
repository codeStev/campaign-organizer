package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.UpdateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;

public interface UpdateArcUseCase {

    ArcView update(UpdateArcCommand command);
}
