package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.in.HandoutCommands.CreateHandoutCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutView;

public interface CreateHandoutUseCase {

    HandoutView create(CreateHandoutCommand command);
}
