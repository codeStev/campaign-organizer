package com.campaignorganizer.campaign.application.session.port.published;

import java.util.Optional;
import java.util.UUID;

/** Cross-context read access to session cheat sheets (published; ADR-0050). */
public interface CheatSheetQueryPort {

    Optional<CheatSheetView> findBySession(UUID sessionId);

    boolean existsBySession(UUID sessionId);
}
