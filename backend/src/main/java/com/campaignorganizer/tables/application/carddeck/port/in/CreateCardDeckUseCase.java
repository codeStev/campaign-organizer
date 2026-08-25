package com.campaignorganizer.tables.application.carddeck.port.in;

import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CreateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;

public interface CreateCardDeckUseCase {

    CardDeckView create(CreateCardDeckCommand command);
}
