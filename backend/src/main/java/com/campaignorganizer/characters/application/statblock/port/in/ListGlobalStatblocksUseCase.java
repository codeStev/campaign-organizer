package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import java.util.List;
import java.util.UUID;

public interface ListGlobalStatblocksUseCase {

    /** Lists the global catalog; when {@code systemId} is set, scopes to that game system. */
    List<GlobalStatblockView> list(UUID systemId);
}
