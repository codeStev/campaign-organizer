package com.campaignorganizer.campaign.application.beatkind.port.in;

import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import java.util.UUID;

public interface GetBeatKindUseCase {

    BeatKindView get(UUID worldId, UUID beatKindId);
}
