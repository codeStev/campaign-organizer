package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.UpdateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;

public interface UpdateHandoutCategoryUseCase {

    HandoutCategoryView update(UpdateHandoutCategoryCommand command);
}
