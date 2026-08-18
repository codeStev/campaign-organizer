package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import java.util.List;
import java.util.UUID;

public interface ListSessionsUseCase {

    List<SessionView> list(UUID worldId, UUID campaignId);
}
