package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;
import java.util.List;
import java.util.UUID;

public interface ListMapCategoriesUseCase {

    List<MapCategoryView> list(UUID worldId);
}
