package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import java.util.UUID;

public interface GetCheatSheetUseCase {

    /**
     * The session's sheet; never null — a view with {@code id == null} when
     * none was saved yet, so clients can render an empty editor in one call.
     */
    CheatSheetView get(UUID worldId, UUID campaignId, UUID sessionId);
}
