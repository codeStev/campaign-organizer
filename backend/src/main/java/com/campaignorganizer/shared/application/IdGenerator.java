package com.campaignorganizer.shared.application;

import java.util.UUID;

/**
 * Outbound port for identifier generation, so aggregates receive their id from the
 * application layer instead of {@code UUID.randomUUID()} in an entity — keeping the
 * domain deterministic and unit-testable (ADR-0049 analysis, finding F3).
 */
public interface IdGenerator {

    UUID newId();
}
