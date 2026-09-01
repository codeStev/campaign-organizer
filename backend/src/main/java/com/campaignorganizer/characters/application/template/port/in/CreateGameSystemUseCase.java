package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.CreateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;

public interface CreateGameSystemUseCase {

    GameSystemView create(CreateGameSystemCommand command);
}
