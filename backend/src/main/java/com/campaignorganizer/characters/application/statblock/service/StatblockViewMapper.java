package com.campaignorganizer.characters.application.statblock.service;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.domain.statblock.Statblock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatblockViewMapper {

    StatblockView toView(Statblock statblock);
}
