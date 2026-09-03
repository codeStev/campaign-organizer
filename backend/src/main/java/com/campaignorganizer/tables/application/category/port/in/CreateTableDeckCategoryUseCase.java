package com.campaignorganizer.tables.application.category.port.in;

import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.CreateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;

public interface CreateTableDeckCategoryUseCase {

    TableDeckCategoryView create(CreateTableDeckCategoryCommand command);
}
