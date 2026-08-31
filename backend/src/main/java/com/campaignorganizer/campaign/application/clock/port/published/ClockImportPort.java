package com.campaignorganizer.campaign.application.clock.port.published;

/**
 * Published port: persists a clock exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id - backup
 * import's counterpart to the normal create flow (ADR-0061, mirrors
 * ArcImportPort).
 */
public interface ClockImportPort {

    ClockView importClock(ClockView view);
}
