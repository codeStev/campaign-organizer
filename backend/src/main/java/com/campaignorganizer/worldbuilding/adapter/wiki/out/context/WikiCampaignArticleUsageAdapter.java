package com.campaignorganizer.worldbuilding.adapter.wiki.out.context;

import com.campaignorganizer.usage.UsageService;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.CampaignArticleUsagePort;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: article ids referenced by a campaign, via the legacy usage service (ADR-0033). */
@Component
public class WikiCampaignArticleUsageAdapter implements CampaignArticleUsagePort {

    private final UsageService usageService;

    public WikiCampaignArticleUsageAdapter(UsageService usageService) {
        this.usageService = usageService;
    }

    @Override
    public Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId) {
        return usageService.articleIdsUsedInCampaign(worldId, campaignId);
    }
}
