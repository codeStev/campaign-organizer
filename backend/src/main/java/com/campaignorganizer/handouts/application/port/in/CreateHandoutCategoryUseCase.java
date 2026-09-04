package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.CreateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;

public interface CreateHandoutCategoryUseCase {

    HandoutCategoryView create(CreateHandoutCategoryCommand command);
}
