package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import java.util.List;
import java.util.UUID;

public interface ListBeatsUseCase {

    List<ArcBeatView> list(UUID worldId, UUID campaignId, UUID arcId);
}
