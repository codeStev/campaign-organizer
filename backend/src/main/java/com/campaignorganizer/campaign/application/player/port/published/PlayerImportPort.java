package com.campaignorganizer.campaign.application.player.port.published;

/**
 * Published port: persists a player exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface PlayerImportPort {

    PlayerView importPlayer(PlayerView view);
}
