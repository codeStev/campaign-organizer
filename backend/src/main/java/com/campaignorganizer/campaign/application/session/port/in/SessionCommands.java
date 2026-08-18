package com.campaignorganizer.campaign.application.session.port.in;

import java.time.LocalDate;
import java.util.UUID;

public final class SessionCommands {

    private SessionCommands() {
    }

    public record CreateSessionCommand(UUID worldId, UUID campaignId, String title,
                                       Integer sessionNumber, LocalDate date, String summary,
                                       String notes) {
    }

    public record UpdateSessionCommand(UUID worldId, UUID campaignId, UUID sessionId, String title,
                                       Integer sessionNumber, LocalDate date, String summary,
                                       String notes) {
    }
}
