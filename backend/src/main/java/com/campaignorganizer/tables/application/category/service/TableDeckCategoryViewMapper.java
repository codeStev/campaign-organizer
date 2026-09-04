package com.campaignorganizer.tables.application.category.service;

import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;
import com.campaignorganizer.tables.domain.category.TableDeckCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableDeckCategoryViewMapper {

    TableDeckCategoryView toView(TableDeckCategory category);
}
