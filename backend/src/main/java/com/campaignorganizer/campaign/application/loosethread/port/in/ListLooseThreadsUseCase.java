package com.campaignorganizer.campaign.application.loosethread.port.in;

import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import java.util.List;
import java.util.UUID;

public interface ListLooseThreadsUseCase {

    List<LooseThreadView> list(UUID worldId, UUID campaignId, UUID sessionId);
}
