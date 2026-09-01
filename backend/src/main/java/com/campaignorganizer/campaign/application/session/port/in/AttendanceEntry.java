package com.campaignorganizer.campaign.application.session.port.in;

import java.util.UUID;

/** One attendance row, with the player's name/guest flag and character name denormalized. */
public record AttendanceEntry(UUID playerId, String name, boolean guest, boolean present,
                              UUID characterId, String characterName) {
}
