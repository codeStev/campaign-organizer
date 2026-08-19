package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.CreateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;

public interface CreateCharacterSheetUseCase {

    CharacterSheetView create(CreateCharacterSheetCommand command);
}
