package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.domain.map.WorldMap;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapViewMapper {

    MapView toView(WorldMap map);
}
