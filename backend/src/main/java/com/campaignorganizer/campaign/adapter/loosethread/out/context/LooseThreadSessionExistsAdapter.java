package com.campaignorganizer.campaign.adapter.loosethread.out.context;

import com.campaignorganizer.campaign.application.loosethread.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves session existence for the loose-thread module via the session query port. */
@Component
public class LooseThreadSessionExistsAdapter implements SessionExistsPort {

    private final SessionQueryPort sessions;

    public LooseThreadSessionExistsAdapter(SessionQueryPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean existsInCampaign(UUID sessionId, UUID campaignId) {
        return sessions.existsInCampaign(sessionId, campaignId);
    }
}
