package com.campaignorganizer.characters.application.sheet.service;

import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.domain.sheet.CharacterSheet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CharacterSheetViewMapper {

    CharacterSheetView toView(CharacterSheet sheet);
}
