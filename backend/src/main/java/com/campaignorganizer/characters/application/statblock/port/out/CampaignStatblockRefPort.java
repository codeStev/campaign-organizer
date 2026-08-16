package com.campaignorganizer.characters.application.statblock.port.out;

import java.util.List;
import java.util.UUID;

/** ACL into the campaign context: statblock ids referenced by a campaign's beats (ADR-0043). */
public interface CampaignStatblockRefPort {

    List<UUID> statblockIdsReferencedByCampaign(UUID campaignId);
}
