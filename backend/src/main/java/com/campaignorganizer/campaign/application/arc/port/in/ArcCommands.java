package com.campaignorganizer.campaign.application.arc.port.in;

import com.campaignorganizer.campaign.domain.arc.ArcStatus;
import java.util.UUID;

public final class ArcCommands {

    private ArcCommands() {
    }

    /** {@code status}/{@code position} may be null; the service defaults them (ADR-0032). */
    public record CreateArcCommand(UUID worldId, UUID campaignId, String title, String description,
                                   ArcStatus status, Integer position) {
    }

    public record UpdateArcCommand(UUID worldId, UUID campaignId, UUID arcId, String title,
                                   String description, ArcStatus status, Integer position) {
    }
}
