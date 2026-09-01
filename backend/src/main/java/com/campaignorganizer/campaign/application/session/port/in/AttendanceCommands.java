package com.campaignorganizer.campaign.application.session.port.in;

import java.util.List;
import java.util.UUID;

public final class AttendanceCommands {

    private AttendanceCommands() {
    }

    public record AttendanceEntryInput(UUID playerId, boolean present, UUID characterId) {
    }

    public record PutSessionAttendanceCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                              List<AttendanceEntryInput> entries) {
    }
}
