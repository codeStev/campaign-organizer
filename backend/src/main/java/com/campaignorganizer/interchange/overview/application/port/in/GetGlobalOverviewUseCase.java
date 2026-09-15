package com.campaignorganizer.interchange.overview.application.port.in;

import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.GlobalOverviewStats;

public interface GetGlobalOverviewUseCase {

    /** Scoped to the caller's own account (issue #68) — no id, no cross-account leak surface. */
    GlobalOverviewStats overview();
}
