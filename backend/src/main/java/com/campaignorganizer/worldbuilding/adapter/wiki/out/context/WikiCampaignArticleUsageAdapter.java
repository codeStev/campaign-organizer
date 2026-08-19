package com.campaignorganizer.worldbuilding.adapter.wiki.out.context;

import com.campaignorganizer.interchange.usage.application.port.published.UsageQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.CampaignArticleUsagePort;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: article ids referenced by a campaign, via the interchange usage query port (ADR-0033). */
@Component
public class WikiCampaignArticleUsageAdapter implements CampaignArticleUsagePort {

    private final UsageQueryPort usage;

    public WikiCampaignArticleUsageAdapter(UsageQueryPort usage) {
        this.usage = usage;
    }

    @Override
    public Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId) {
        return usage.articleIdsUsedInCampaign(worldId, campaignId);
    }
}
