package com.campaignorganizer.characters.application.category.service;

import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;
import com.campaignorganizer.characters.domain.category.SheetCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetCategoryViewMapper {

    SheetCategoryView toView(SheetCategory category);
}
