package com.campaignorganizer.campaign.application.beatkind.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read beat kinds from sibling modules (beat kind-tag validation, world export). */
public interface BeatKindQueryPort {

    List<BeatKindView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID beatKindId, UUID worldId);
}
