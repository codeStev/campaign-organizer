package com.campaignorganizer.campaign.application.beatkind.port.in;

import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.CreateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;

public interface CreateBeatKindUseCase {

    BeatKindView create(CreateBeatKindCommand command);
}
