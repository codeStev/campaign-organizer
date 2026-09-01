package com.campaignorganizer.campaign.application.session.port.published;

import java.util.List;
import java.util.UUID;

/** Cross-context read access to session attendance (published; ADR-0091). */
public interface SessionAttendanceQueryPort {

    List<SessionAttendanceView> findBySession(UUID sessionId);
}
