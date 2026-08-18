package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.CreateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;

public interface CreateSessionUseCase {

    SessionView create(CreateSessionCommand command);
}
