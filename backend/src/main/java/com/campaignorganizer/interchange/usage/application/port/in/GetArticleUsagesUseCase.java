package com.campaignorganizer.interchange.usage.application.port.in;

import com.campaignorganizer.interchange.usage.application.port.in.UsageDtos.UsageResponse;
import java.util.UUID;

public interface GetArticleUsagesUseCase {

    UsageResponse articleUsages(UUID worldId, UUID articleId);
}
