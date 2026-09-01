package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.characters.domain.template.GameSystem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameSystemViewMapper {

    GameSystemView toView(GameSystem system);
}
