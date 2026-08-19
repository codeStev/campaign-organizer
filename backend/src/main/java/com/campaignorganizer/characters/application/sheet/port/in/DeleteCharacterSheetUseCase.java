package com.campaignorganizer.characters.application.sheet.port.in;

import java.util.UUID;

public interface DeleteCharacterSheetUseCase {

    void delete(UUID worldId, UUID sheetId);
}
