package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.UpdateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;

public interface UpdateStatblockUseCase {

    StatblockView update(UpdateStatblockCommand command);
}
