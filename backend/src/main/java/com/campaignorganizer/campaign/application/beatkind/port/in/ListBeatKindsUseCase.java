package com.campaignorganizer.campaign.application.beatkind.port.in;

import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import java.util.List;
import java.util.UUID;

public interface ListBeatKindsUseCase {

    List<BeatKindView> list(UUID worldId);
}
