package com.campaignorganizer.accounts.application.mfa.port.out;

import java.util.List;

/**
 * Generates plaintext recovery codes (ADR-0111) — kept out of {@code MfaService}, same
 * reasoning as {@link com.campaignorganizer.shared.application.IdGenerator}: randomness stays
 * behind a port so the service is deterministic and testable.
 */
public interface RecoveryCodePort {

    List<String> generateCodes(int count);
}
