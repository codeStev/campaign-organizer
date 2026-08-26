package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.in.HandoutCommands.UpdateHandoutCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutView;

public interface UpdateHandoutUseCase {

    HandoutView update(UpdateHandoutCommand command);
}
