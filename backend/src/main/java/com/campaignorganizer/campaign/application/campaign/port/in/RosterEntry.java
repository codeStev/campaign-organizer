package com.campaignorganizer.campaign.application.campaign.port.in;

import java.util.UUID;

/** One roster row, with the player's name denormalized for display. */
public record RosterEntry(UUID playerId, String name, boolean guest) {
}
