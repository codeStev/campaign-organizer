package com.campaignorganizer.characters.application.statblock.port.in;

import java.util.UUID;

public interface DeleteGlobalStatblockUseCase {

    void delete(UUID globalStatblockId);
}
