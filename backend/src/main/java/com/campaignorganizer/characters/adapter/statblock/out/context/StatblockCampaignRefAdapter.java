package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignStatblockRefPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: statblock ids referenced by a campaign's beats, via the campaign arc query ports. */
@Component
public class StatblockCampaignRefAdapter implements CampaignStatblockRefPort {

    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;

    public StatblockCampaignRefAdapter(ArcQueryPort arcs, ArcBeatQueryPort beats) {
        this.arcs = arcs;
        this.beats = beats;
    }

    @Override
    public List<UUID> statblockIdsReferencedByCampaign(UUID campaignId) {
        List<UUID> arcIds = arcs.findByCampaign(campaignId).stream().map(ArcView::id).toList();
        return beats.linkedStatblockIdsByArcs(arcIds);
    }
}
