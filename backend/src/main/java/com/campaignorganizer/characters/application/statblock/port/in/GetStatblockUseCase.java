package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import java.util.UUID;

public interface GetStatblockUseCase {

    StatblockView get(UUID worldId, UUID statblockId);
}
