package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.PutCheatSheetCommand;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;

public interface PutCheatSheetUseCase {

    CheatSheetView put(PutCheatSheetCommand command);
}
