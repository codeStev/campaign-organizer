package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import java.util.List;
import java.util.UUID;

public interface ListCategoriesUseCase {

    List<CategoryView> list(UUID worldId);
}
