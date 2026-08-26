package com.campaignorganizer.campaign.application.session.port.out;

import com.campaignorganizer.campaign.domain.session.CheatSheet;
import java.util.Optional;
import java.util.UUID;

public interface CheatSheetRepositoryPort {

    Optional<CheatSheet> findBySession(UUID sessionId);

    CheatSheet save(CheatSheet sheet);

    void delete(CheatSheet sheet);
}
