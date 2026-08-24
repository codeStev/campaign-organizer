package com.campaignorganizer.worldbuilding.application.world.port.published;

/**
 * Published port: persists a world exactly as given (id already resolved by
 * the caller) instead of generating a new id — backup import's counterpart
 * to the normal create flow (ADR-0061).
 */
public interface WorldImportPort {

    WorldView importWorld(WorldView view);
}
