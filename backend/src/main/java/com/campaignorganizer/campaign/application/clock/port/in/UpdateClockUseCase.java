package com.campaignorganizer.campaign.application.clock.port.in;

import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.UpdateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;

public interface UpdateClockUseCase {

    ClockView update(UpdateClockCommand command);
}
