package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.UpdateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;

public interface UpdateGameSystemUseCase {

    GameSystemView update(UpdateGameSystemCommand command);
}
