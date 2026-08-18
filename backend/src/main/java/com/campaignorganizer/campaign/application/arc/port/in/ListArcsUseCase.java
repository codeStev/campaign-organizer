package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import java.util.List;
import java.util.UUID;

public interface ListArcsUseCase {

    List<ArcView> list(UUID worldId, UUID campaignId);
}
