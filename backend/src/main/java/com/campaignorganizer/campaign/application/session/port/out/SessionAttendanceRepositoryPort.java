package com.campaignorganizer.campaign.application.session.port.out;

import com.campaignorganizer.campaign.domain.session.SessionAttendance;
import java.util.List;
import java.util.UUID;

public interface SessionAttendanceRepositoryPort {

    List<SessionAttendance> findBySession(UUID sessionId);

    SessionAttendance save(SessionAttendance row);

    /** Deletes every existing attendance row for this session, for a whole-set replace. */
    void deleteBySession(UUID sessionId);
}
