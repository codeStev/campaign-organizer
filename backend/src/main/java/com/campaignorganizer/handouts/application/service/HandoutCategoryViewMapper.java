package com.campaignorganizer.handouts.application.service;

import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;
import com.campaignorganizer.handouts.domain.HandoutCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HandoutCategoryViewMapper {

    HandoutCategoryView toView(HandoutCategory category);
}
