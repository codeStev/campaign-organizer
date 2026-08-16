package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.campaign.Arc;
import com.campaignorganizer.campaign.ArcBeatRepository;
import com.campaignorganizer.campaign.ArcRepository;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignStatblockRefPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: statblock ids referenced by a campaign's beats, via the legacy campaign repositories. */
@Component
public class StatblockCampaignRefAdapter implements CampaignStatblockRefPort {

    private final ArcRepository arcs;
    private final ArcBeatRepository beats;

    public StatblockCampaignRefAdapter(ArcRepository arcs, ArcBeatRepository beats) {
        this.arcs = arcs;
        this.beats = beats;
    }

    @Override
    public List<UUID> statblockIdsReferencedByCampaign(UUID campaignId) {
        List<UUID> arcIds = arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId)
                .stream().map(Arc::getId).toList();
        if (arcIds.isEmpty()) {
            return List.of();
        }
        return beats.findLinkedStatblockIdsByArcIds(arcIds);
    }
}
