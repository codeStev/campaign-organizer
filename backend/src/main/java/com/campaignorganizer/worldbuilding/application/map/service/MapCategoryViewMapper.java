package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;
import com.campaignorganizer.worldbuilding.domain.map.MapCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapCategoryViewMapper {

    MapCategoryView toView(MapCategory category);
}
