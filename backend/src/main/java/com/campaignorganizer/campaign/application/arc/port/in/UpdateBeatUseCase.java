package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.UpdateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;

public interface UpdateBeatUseCase {

    ArcBeatView update(UpdateBeatCommand command);
}
