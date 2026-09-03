package com.campaignorganizer.campaign.application.beatkind.port.in;

import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.UpdateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;

public interface UpdateBeatKindUseCase {

    BeatKindView update(UpdateBeatKindCommand command);
}
