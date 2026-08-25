package com.campaignorganizer.interchange.usage.application.port.in;

import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ConsistencyReport;
import java.util.UUID;

/** FR-43: lint a whole world for broken links, orphans and campaign-orphans. */
public interface GetConsistencyReportUseCase {

    ConsistencyReport report(UUID worldId);
}
