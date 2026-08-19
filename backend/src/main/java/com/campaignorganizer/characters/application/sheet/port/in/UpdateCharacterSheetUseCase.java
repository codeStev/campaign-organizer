package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.UpdateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;

public interface UpdateCharacterSheetUseCase {

    CharacterSheetView update(UpdateCharacterSheetCommand command);
}
