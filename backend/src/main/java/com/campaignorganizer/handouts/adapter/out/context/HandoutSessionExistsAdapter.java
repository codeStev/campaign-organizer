package com.campaignorganizer.handouts.adapter.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.handouts.application.port.out.SessionExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Session-in-world check against the campaign context's published ports.
 * Handouts only carry a worldId (no campaignId), so this resolves the
 * session's campaign first, then checks that campaign is in the world.
 */
@Component
public class HandoutSessionExistsAdapter implements SessionExistsPort {

    private final SessionQueryPort sessions;
    private final CampaignQueryPort campaigns;

    public HandoutSessionExistsAdapter(SessionQueryPort sessions, CampaignQueryPort campaigns) {
        this.sessions = sessions;
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID sessionId, UUID worldId) {
        return sessions.findById(sessionId)
                .map(SessionView::campaignId)
                .map(campaignId -> campaigns.existsInWorld(campaignId, worldId))
                .orElse(false);
    }
}
