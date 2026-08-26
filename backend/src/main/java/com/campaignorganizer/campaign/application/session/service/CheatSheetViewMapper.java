package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.campaign.domain.session.CheatSheet;
import com.campaignorganizer.campaign.domain.session.CheatSheetFragment;
import org.mapstruct.Mapper;

/**
 * Maps the domain cheat sheet to the published read model (MapStruct).
 * Fragments map field-for-field; the domain enum becomes its name.
 */
@Mapper(componentModel = "spring")
public interface CheatSheetViewMapper {

    CheatSheetView toView(CheatSheet sheet);

    default CheatSheetView.FragmentView toFragmentView(CheatSheetFragment fragment) {
        return new CheatSheetView.FragmentView(fragment.id(), fragment.type().name(),
                fragment.text(), fragment.statblockId(), fragment.tableId(), fragment.entryId(),
                fragment.deckId(), fragment.cardId());
    }
}
