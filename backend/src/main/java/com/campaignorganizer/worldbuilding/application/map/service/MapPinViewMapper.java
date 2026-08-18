package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.domain.map.MapPin;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapPinViewMapper {

    MapPinView toView(MapPin pin);
}
