package com.campaignorganizer.campaign.application.encounter.port.published;

/**
 * Published port: persists an encounter exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id - backup
 * import's counterpart to the normal create flow (ADR-0061, mirrors
 * ClockImportPort). Ordinary campaign data, not a shared catalog, so this is
 * a normal import - no resolve-or-reuse exception like ADR-0093/0096.
 */
public interface EncounterImportPort {

    EncounterView importEncounter(EncounterView view);
}
