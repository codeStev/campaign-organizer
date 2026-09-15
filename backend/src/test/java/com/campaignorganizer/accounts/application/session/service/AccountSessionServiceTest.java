package com.campaignorganizer.accounts.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.session.port.in.AccountSessionSummary;
import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRecord;
import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRepositoryPort;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for self-service session/device tracking (ADR-0112). */
@ExtendWith(MockitoExtension.class)
class AccountSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");

    @Mock
    private AccountSessionRepositoryPort sessions;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();
    private AccountSessionService service;

    @BeforeEach
    void setUp() {
        service = new AccountSessionService(sessions, clock);
    }

    @Test
    void recordingASessionSavesItWithTheCurrentTimeAsCreatedAt() {
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = NOW.plusSeconds(3600);

        service.recordSession(accountId, sessionId, expiresAt, "Mozilla/5.0", "10.0.0.1");

        verify(sessions).save(new AccountSessionRecord(sessionId, accountId, NOW, "Mozilla/5.0", "10.0.0.1",
                expiresAt, null));
    }

    @Test
    void listsOnlyActiveSessionsOldestFirstAndFlagsTheCurrentOne() {
        UUID currentId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID revokedId = UUID.randomUUID();
        UUID expiredId = UUID.randomUUID();
        when(sessions.findByAccountId(accountId)).thenReturn(List.of(
                record(otherId, NOW.minusSeconds(60), null, NOW.plusSeconds(3600)),
                record(currentId, NOW.minusSeconds(120), null, NOW.plusSeconds(3600)),
                record(revokedId, NOW.minusSeconds(30), NOW, NOW.plusSeconds(3600)),
                record(expiredId, NOW.minusSeconds(30), null, NOW.minusSeconds(1))));

        List<AccountSessionSummary> result = service.listSessions(accountId, currentId);

        assertThat(result).extracting(AccountSessionSummary::id).containsExactly(currentId, otherId);
        assertThat(result).filteredOn(s -> s.id().equals(currentId)).extracting(AccountSessionSummary::current)
                .containsExactly(true);
        assertThat(result).filteredOn(s -> s.id().equals(otherId)).extracting(AccountSessionSummary::current)
                .containsExactly(false);
    }

    @Test
    void revokingAnExistingSessionDelegatesToTheRepository() {
        UUID sessionId = UUID.randomUUID();
        when(sessions.revokeByIdForAccount(accountId, sessionId, NOW)).thenReturn(true);

        service.revokeSession(accountId, sessionId);

        verify(sessions).revokeByIdForAccount(accountId, sessionId, NOW);
    }

    @Test
    void revokingAnUnknownOrForeignSessionFails() {
        UUID sessionId = UUID.randomUUID();
        when(sessions.revokeByIdForAccount(accountId, sessionId, NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.revokeSession(accountId, sessionId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void aSessionIsActiveOnlyWhenNotRevokedAndNotExpired() {
        UUID active = UUID.randomUUID();
        UUID revoked = UUID.randomUUID();
        UUID expired = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(sessions.findById(active)).thenReturn(Optional.of(record(active, NOW, null, NOW.plusSeconds(1))));
        when(sessions.findById(revoked)).thenReturn(Optional.of(record(revoked, NOW, NOW, NOW.plusSeconds(3600))));
        when(sessions.findById(expired)).thenReturn(Optional.of(record(expired, NOW, null, NOW.minusSeconds(1))));
        when(sessions.findById(unknown)).thenReturn(Optional.empty());

        assertThat(service.isActive(active)).isTrue();
        assertThat(service.isActive(revoked)).isFalse();
        assertThat(service.isActive(expired)).isFalse();
        assertThat(service.isActive(unknown)).isFalse();
    }

    @Test
    void recordingNeverConsultsFindByIdOrAccountId() {
        service.recordSession(accountId, UUID.randomUUID(), NOW.plusSeconds(60), null, null);

        verify(sessions, never()).findById(any());
        verify(sessions, never()).findByAccountId(any());
    }

    private static AccountSessionRecord record(UUID id, Instant createdAt, Instant revokedAt, Instant expiresAt) {
        return new AccountSessionRecord(id, UUID.randomUUID(), createdAt, "ua", "ip", expiresAt, revokedAt);
    }
}
