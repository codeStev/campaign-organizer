package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** One status query serves every pushable entity type's "last pushed at ..." UI indicator. */
public interface GetFoundryPushStatusUseCase {

    Optional<FoundryPushStatusView> statusFor(UUID worldId, FoundryEntityType entityType, UUID entityId);

    record FoundryPushStatusView(String foundryDocumentId, Instant pushedAt) {
    }
}
