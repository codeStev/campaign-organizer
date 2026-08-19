package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import java.util.UUID;

public interface GetCharacterSheetUseCase {

    CharacterSheetView get(UUID worldId, UUID sheetId);
}
