package com.campaignorganizer.tables.application.category.port.in;

import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.UpdateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;

public interface UpdateTableDeckCategoryUseCase {

    TableDeckCategoryView update(UpdateTableDeckCategoryCommand command);
}
