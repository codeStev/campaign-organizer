package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryViewMapper {

    CategoryView toView(Category category);
}
