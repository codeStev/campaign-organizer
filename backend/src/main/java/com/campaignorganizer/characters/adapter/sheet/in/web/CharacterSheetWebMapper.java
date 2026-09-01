package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.in.web.CharacterSheetWebDtos.CharacterSheetRequest;
import com.campaignorganizer.characters.adapter.sheet.in.web.CharacterSheetWebDtos.CharacterSheetResponse;
import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.CreateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.UpdateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CharacterSheetWebMapper {

    CharacterSheetResponse toResponse(CharacterSheetView view);

    default CreateCharacterSheetCommand toCreateCommand(UUID worldId, CharacterSheetRequest request) {
        return new CreateCharacterSheetCommand(worldId, request.worldTemplateId(), request.globalTemplateId(),
                request.articleId(), request.campaignId(), request.name(), request.values());
    }

    default UpdateCharacterSheetCommand toUpdateCommand(UUID worldId, UUID sheetId,
                                                         CharacterSheetRequest request) {
        return new UpdateCharacterSheetCommand(worldId, sheetId, request.worldTemplateId(),
                request.globalTemplateId(), request.articleId(), request.campaignId(), request.name(),
                request.values());
    }
}
