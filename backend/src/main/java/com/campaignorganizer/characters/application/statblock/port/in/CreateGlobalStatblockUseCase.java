package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.CreateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;

public interface CreateGlobalStatblockUseCase {

    GlobalStatblockView create(CreateGlobalStatblockCommand command);
}
