package com.campaignorganizer.campaign.application.session.port.published;

/** Published import for backup/restore (ADR-0061): saves a cheat sheet verbatim. */
public interface CheatSheetImportPort {

    CheatSheetView importCheatSheet(CheatSheetView view);
}
