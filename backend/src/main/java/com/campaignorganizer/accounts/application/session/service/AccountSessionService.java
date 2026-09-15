package com.campaignorganizer.accounts.application.session.service;

import com.campaignorganizer.accounts.application.session.port.in.AccountSessionSummary;
import com.campaignorganizer.accounts.application.session.port.in.ListAccountSessionsUseCase;
import com.campaignorganizer.accounts.application.session.port.in.RecordAccountSessionUseCase;
import com.campaignorganizer.accounts.application.session.port.in.RevokeAccountSessionUseCase;
import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRecord;
import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRepositoryPort;
import com.campaignorganizer.accounts.application.session.port.published.AccountSessionQueryPort;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service session/device tracking (ADR-0112) — a second, finer-grained revocation layer
 * alongside {@code Account.tokenVersion}, not a replacement: an account-wide revocation (e.g.
 * {@code logoutAll}, a password change) still works exactly as before purely via the
 * {@code tokenVersion} check in {@code JwtAuthFilter}, independent of this table's state.
 */
@Service
public class AccountSessionService implements ListAccountSessionsUseCase, RevokeAccountSessionUseCase,
        RecordAccountSessionUseCase, AccountSessionQueryPort {

    private final AccountSessionRepositoryPort sessions;
    private final Clock clock;

    public AccountSessionService(AccountSessionRepositoryPort sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSessionSummary> listSessions(UUID accountId, UUID currentSessionId) {
        Instant now = clock.instant();
        return sessions.findByAccountId(accountId).stream()
                .filter(session -> session.revokedAt() == null && session.expiresAt().isAfter(now))
                .sorted(Comparator.comparing(AccountSessionRecord::createdAt))
                .map(session -> new AccountSessionSummary(session.id(), session.createdAt(), session.userAgent(),
                        session.ipAddress(), session.expiresAt(), session.id().equals(currentSessionId)))
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID accountId, UUID sessionId) {
        boolean revoked = sessions.revokeByIdForAccount(accountId, sessionId, clock.instant());
        if (!revoked) {
            throw new NotFoundException("Session not found");
        }
    }

    @Override
    @Transactional
    public void recordSession(UUID accountId, UUID sessionId, Instant expiresAt, String userAgent, String ipAddress) {
        sessions.save(new AccountSessionRecord(sessionId, accountId, clock.instant(), userAgent, ipAddress,
                expiresAt, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId) {
        Instant now = clock.instant();
        return sessions.findById(sessionId)
                .filter(session -> session.revokedAt() == null)
                .filter(session -> session.expiresAt().isAfter(now))
                .isPresent();
    }
}
