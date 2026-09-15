package com.campaignorganizer.accounts.application.session.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Raw storage access for {@code account_sessions} — {@code AccountSessionService} applies the active/expired rules. */
public interface AccountSessionRepositoryPort {

    void save(AccountSessionRecord session);

    Optional<AccountSessionRecord> findById(UUID sessionId);

    /** Ordered by creation date, including revoked/expired rows — the service filters as needed. */
    List<AccountSessionRecord> findByAccountId(UUID accountId);

    /**
     * Marks exactly one not-yet-revoked session revoked, scoped to the given account — a session
     * id from one account can never revoke another's, even if guessed.
     *
     * @return true if a matching, still-active row was found and revoked, false otherwise
     */
    boolean revokeByIdForAccount(UUID accountId, UUID sessionId, Instant revokedAt);
}
