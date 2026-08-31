package com.campaignorganizer.campaign.application.clock.port.in;

import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.CreateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;

public interface CreateClockUseCase {

    ClockView create(CreateClockCommand command);
}
