package com.campaignorganizer.campaign.application.loosethread.port.in;

import com.campaignorganizer.campaign.domain.loosethread.LooseThreadStatus;
import java.util.UUID;

public final class LooseThreadCommands {

    private LooseThreadCommands() {
    }

    public record CreateLooseThreadCommand(UUID worldId, UUID campaignId, UUID sessionId, String text,
                                            LooseThreadStatus status) {
    }

    public record UpdateLooseThreadCommand(UUID worldId, UUID campaignId, UUID sessionId, UUID threadId,
                                            String text, LooseThreadStatus status) {
    }
}
