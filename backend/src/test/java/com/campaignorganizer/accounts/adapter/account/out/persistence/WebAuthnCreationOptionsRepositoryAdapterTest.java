package com.campaignorganizer.accounts.adapter.account.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.security.CurrentUserPort;
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
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;

/**
 * Unit coverage for the save-then-load round trip {@code WebAuthnCeremonyWiringIT} never
 * exercised (it only asserts on the raw HTTP options response, never that this adapter can read
 * back what it just wrote) — the gap that let a real Jackson allowlist deserialization bug ship
 * and break every WebAuthn registration attempt in production before it was caught.
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnCreationOptionsRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private WebAuthnChallengeJpaRepository repository;
    @Mock
    private CurrentUserPort currentUser;
    @Captor
    private ArgumentCaptor<WebAuthnChallengeJpaEntity> entityCaptor;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private WebAuthnCreationOptionsRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WebAuthnCreationOptionsRepositoryAdapter(repository, currentUser, clock);
    }

    @Test
    void aSavedOptionsObjectRoundTripsThroughLoadUnchanged() {
        when(currentUser.currentAccountId()).thenReturn(ACCOUNT_ID);
        PublicKeyCredentialCreationOptions original = realisticOptions();

        // Capture what save() actually persists, then feed it back through findById the same
        // way a real load() call would — this is the exact path that crashed in production.
        adapter.save(null, null, original);
        verify(repository).save(entityCaptor.capture());
        when(repository.findById(ACCOUNT_ID)).thenReturn(Optional.of(entityCaptor.getValue()));

        PublicKeyCredentialCreationOptions loaded = adapter.load(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getChallenge()).isEqualTo(original.getChallenge());
        assertThat(loaded.getUser().getName()).isEqualTo(original.getUser().getName());
        assertThat(loaded.getPubKeyCredParams()).hasSize(original.getPubKeyCredParams().size());
        assertThat(loaded.getRp().getId()).isEqualTo(original.getRp().getId());
    }

    /**
     * Matches the shape Spring's own {@code PublicKeyCredentialCreationOptionsFilter} builds —
     * {@code pubKeyCredParams} in particular is a {@code java.util.Collections$UnmodifiableList}
     * (or similar concrete JDK type) at runtime, which is exactly what the allowlist
     * deserializer rejected without {@code SecurityJackson2Modules} registered.
     */
    private static PublicKeyCredentialCreationOptions realisticOptions() {
        PublicKeyCredentialUserEntity user = ImmutablePublicKeyCredentialUserEntity.builder()
                .id(new Bytes(ACCOUNT_ID.toString().getBytes()))
                .name("gm@example.com")
                .displayName("gm@example.com")
                .build();
        return PublicKeyCredentialCreationOptions.builder()
                .rp(PublicKeyCredentialRpEntity.builder().id("localhost").name("Campaign Organizer").build())
                .user(user)
                .challenge(Bytes.random())
                .pubKeyCredParams(java.util.List.of(
                        PublicKeyCredentialParameters.ES256, PublicKeyCredentialParameters.RS256))
                .build();
    }
}
