package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.CreateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;

public interface CreateBeatUseCase {

    ArcBeatView create(CreateBeatCommand command);
}
