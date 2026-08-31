package com.campaignorganizer.campaign.application.loosethread.port.published;

/**
 * Published port: persists a loose thread exactly as given (id and foreign
 * keys already resolved by the caller) instead of generating a new id -
 * backup import's counterpart to the normal create flow (ADR-0061, mirrors
 * ArcImportPort/ClockImportPort).
 */
public interface LooseThreadImportPort {

    LooseThreadView importLooseThread(LooseThreadView view);
}
