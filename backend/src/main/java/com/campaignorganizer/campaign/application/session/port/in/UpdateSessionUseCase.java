package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.UpdateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;

public interface UpdateSessionUseCase {

    SessionView update(UpdateSessionCommand command);
}
