package com.campaignorganizer.campaign.application.session.port.in;

import java.util.List;
import java.util.UUID;

public interface GetSessionAttendanceUseCase {

    List<AttendanceEntry> get(UUID worldId, UUID campaignId, UUID sessionId);
}
