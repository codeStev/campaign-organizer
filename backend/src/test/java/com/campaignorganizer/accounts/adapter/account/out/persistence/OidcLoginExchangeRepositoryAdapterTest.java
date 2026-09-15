package com.campaignorganizer.accounts.adapter.account.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for the single-use OIDC login redirect handoff (ADR-0113). */
@ExtendWith(MockitoExtension.class)
class OidcLoginExchangeRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");

    @Mock
    private OidcLoginExchangeJpaRepository repository;
    @Captor
    private ArgumentCaptor<OidcLoginExchangeJpaEntity> entityCaptor;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OidcLoginExchangeRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OidcLoginExchangeRepositoryAdapter(repository, clock);
    }

    @Test
    void stagingSavesTheJsonWithAFreshRandomCode() {
        Instant expiresAt = NOW.plusSeconds(120);

        UUID code = adapter.stage("{\"status\":\"MFA_SETUP_REQUIRED\"}", expiresAt);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCode()).isEqualTo(code);
        assertThat(entityCaptor.getValue().getLoginResponseJson()).isEqualTo("{\"status\":\"MFA_SETUP_REQUIRED\"}");
        assertThat(entityCaptor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void consumingAnExistingUnexpiredCodeReturnsItsJsonAndDeletesTheRow() {
        UUID code = UUID.randomUUID();
        when(repository.findById(code)).thenReturn(Optional.of(entity(code, NOW.plusSeconds(60))));

        Optional<String> result = adapter.consume(code);

        assertThat(result).contains("{\"status\":\"MFA_SETUP_REQUIRED\"}");
        verify(repository).deleteById(code);
    }

    @Test
    void consumingAnUnknownCodeReturnsEmpty() {
        UUID code = UUID.randomUUID();
        when(repository.findById(code)).thenReturn(Optional.empty());

        assertThat(adapter.consume(code)).isEmpty();
        verify(repository, org.mockito.Mockito.never()).deleteById(any());
    }

    @Test
    void consumingAnExpiredCodeReturnsEmptyAndLeavesTheRowAlone() {
        UUID code = UUID.randomUUID();
        when(repository.findById(code)).thenReturn(Optional.of(entity(code, NOW.minusSeconds(1))));

        assertThat(adapter.consume(code)).isEmpty();
        verify(repository, org.mockito.Mockito.never()).deleteById(any());
    }

    private static OidcLoginExchangeJpaEntity entity(UUID code, Instant expiresAt) {
        OidcLoginExchangeJpaEntity entity = new OidcLoginExchangeJpaEntity();
        entity.setCode(code);
        entity.setLoginResponseJson("{\"status\":\"MFA_SETUP_REQUIRED\"}");
        entity.setExpiresAt(expiresAt);
        return entity;
    }
}
