package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import java.util.UUID;

public interface GetGlobalStatblockUseCase {

    GlobalStatblockView get(UUID globalStatblockId);
}
