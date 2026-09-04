package com.campaignorganizer.campaign.application.beatkind.port.published;

/**
 * Published port: persists a beat kind exactly as given (id and world id
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface BeatKindImportPort {

    BeatKindView importBeatKind(BeatKindView view);
}
