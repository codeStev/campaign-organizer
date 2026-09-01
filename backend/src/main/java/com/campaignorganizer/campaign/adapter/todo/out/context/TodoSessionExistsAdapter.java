package com.campaignorganizer.campaign.adapter.todo.out.context;

import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.todo.port.out.SessionExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves session existence for the todo module via the session query port. */
@Component
public class TodoSessionExistsAdapter implements SessionExistsPort {

    private final SessionQueryPort sessions;

    public TodoSessionExistsAdapter(SessionQueryPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean existsInCampaign(UUID sessionId, UUID campaignId) {
        return sessions.existsInCampaign(sessionId, campaignId);
    }
}
