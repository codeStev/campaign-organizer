package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;
import java.util.List;
import java.util.UUID;

public interface ListHandoutCategoriesUseCase {

    List<HandoutCategoryView> list(UUID worldId);
}
