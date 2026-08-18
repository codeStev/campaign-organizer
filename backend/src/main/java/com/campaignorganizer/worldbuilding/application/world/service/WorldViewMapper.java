package com.campaignorganizer.worldbuilding.application.world.service;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import com.campaignorganizer.worldbuilding.domain.world.World;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorldViewMapper {

    WorldView toView(World world);
}
