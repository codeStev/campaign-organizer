package com.campaignorganizer.characters.application.statblock.service;

import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalStatblockViewMapper {

    GlobalStatblockView toView(GlobalStatblock statblock);
}
