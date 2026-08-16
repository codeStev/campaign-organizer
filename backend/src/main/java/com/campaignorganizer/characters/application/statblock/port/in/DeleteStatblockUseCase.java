package com.campaignorganizer.characters.application.statblock.port.in;

import java.util.UUID;

public interface DeleteStatblockUseCase {

    void delete(UUID worldId, UUID statblockId);
}
