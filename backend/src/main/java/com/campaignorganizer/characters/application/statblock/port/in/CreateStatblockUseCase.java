package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;

public interface CreateStatblockUseCase {

    StatblockView create(CreateStatblockCommand command);
}
