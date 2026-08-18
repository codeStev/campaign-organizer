package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.util.List;

public interface ListWorldsUseCase {

    List<WorldView> list();
}
