package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.UpdateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;

public interface UpdateGlobalStatblockUseCase {

    GlobalStatblockView update(UpdateGlobalStatblockCommand command);
}
