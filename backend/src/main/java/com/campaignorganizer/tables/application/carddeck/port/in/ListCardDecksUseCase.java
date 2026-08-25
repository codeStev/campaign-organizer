package com.campaignorganizer.tables.application.carddeck.port.in;

import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import java.util.List;
import java.util.UUID;

public interface ListCardDecksUseCase {

    List<CardDeckView> list(UUID worldId);
}
