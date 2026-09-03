package com.campaignorganizer.interchange.overview.application.port.in;

import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.WorldOverviewStats;
import java.util.UUID;

public interface GetWorldOverviewUseCase {

    WorldOverviewStats overview(UUID worldId);
}
