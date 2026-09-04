package com.campaignorganizer.characters.application.category.port.in;

import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;
import java.util.List;
import java.util.UUID;

public interface ListSheetCategoriesUseCase {

    List<SheetCategoryView> list(UUID worldId);
}
