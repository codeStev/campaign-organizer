package com.campaignorganizer.tables.application.category.port.in;

import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;
import java.util.List;
import java.util.UUID;

public interface ListTableDeckCategoriesUseCase {

    List<TableDeckCategoryView> list(UUID worldId);
}
