package com.campaignorganizer.tables.application.carddeck.port.in;

import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.UpdateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;

public interface UpdateCardDeckUseCase {

    CardDeckView update(UpdateCardDeckCommand command);
}
