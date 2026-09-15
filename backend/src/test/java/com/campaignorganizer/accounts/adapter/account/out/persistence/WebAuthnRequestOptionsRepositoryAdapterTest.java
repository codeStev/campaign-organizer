package com.campaignorganizer.accounts.adapter.account.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;

/**
 * Unit coverage for the save-then-load round trip {@code WebAuthnCeremonyWiringIT} never
 * exercised — see {@link WebAuthnCreationOptionsRepositoryAdapterTest}'s Javadoc for the
 * production bug this would have caught (the same allowlist-deserializer issue applies to
 * {@code allowCredentials}, a concrete JDK list type at runtime, same as {@code
 * pubKeyCredParams} on the registration side).
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnRequestOptionsRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private WebAuthnChallengeJpaRepository repository;
    @Captor
    private ArgumentCaptor<WebAuthnChallengeJpaEntity> entityCaptor;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private WebAuthnRequestOptionsRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(ACCOUNT_ID, null, List.of()));
        adapter = new WebAuthnRequestOptionsRepositoryAdapter(repository, currentUserPort(), clock);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSavedOptionsObjectRoundTripsThroughLoadUnchanged() {
        PublicKeyCredentialRequestOptions original = realisticOptions();

        adapter.save(null, null, original);
        verify(repository).save(entityCaptor.capture());
        when(repository.findById(ACCOUNT_ID)).thenReturn(Optional.of(entityCaptor.getValue()));

        PublicKeyCredentialRequestOptions loaded = adapter.load(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getChallenge()).isEqualTo(original.getChallenge());
        assertThat(loaded.getRpId()).isEqualTo(original.getRpId());
        assertThat(loaded.getAllowCredentials()).hasSize(original.getAllowCredentials().size());
    }

    /**
     * Matches the shape Spring's own {@code PublicKeyCredentialRequestOptionsFilter} builds —
     * {@code allowCredentials} is a concrete JDK list type at runtime, the login-side analog of
     * the {@code pubKeyCredParams} field that crashed registration in production.
     */
    private static PublicKeyCredentialRequestOptions realisticOptions() {
        PublicKeyCredentialDescriptor credential = PublicKeyCredentialDescriptor.builder()
                .id(Bytes.random())
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .build();
        return PublicKeyCredentialRequestOptions.builder()
                .challenge(Bytes.random())
                .timeout(Duration.ofMinutes(5))
                .rpId("localhost")
                .allowCredentials(List.of(credential))
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
    }

    private static com.campaignorganizer.security.CurrentUserPort currentUserPort() {
        // requireAccountId() reads the SecurityContext directly (see the adapter's own Javadoc
        // on why) rather than delegating to CurrentUserPort — a real bean is still required by
        // the constructor even though this test's path never calls it.
        return org.mockito.Mockito.mock(com.campaignorganizer.security.CurrentUserPort.class);
    }
}
