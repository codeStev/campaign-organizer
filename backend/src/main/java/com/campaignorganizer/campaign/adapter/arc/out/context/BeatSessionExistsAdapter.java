package com.campaignorganizer.campaign.adapter.arc.out.context;

import com.campaignorganizer.campaign.application.arc.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves session existence for the beat module via the session query port. */
@Component
public class BeatSessionExistsAdapter implements SessionExistsPort {

    private final SessionQueryPort sessions;

    public BeatSessionExistsAdapter(SessionQueryPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean existsInCampaign(UUID sessionId, UUID campaignId) {
        return sessions.existsInCampaign(sessionId, campaignId);
    }
}
