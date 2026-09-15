package com.campaignorganizer.accounts.adapter.account.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Unit coverage for the stateless {@code AuthorizationRequestRepository} replacement (ADR-0113)
 * — this app has no {@code HttpSession} for the default implementation to use.
 */
@ExtendWith(MockitoExtension.class)
class OidcAuthorizationRequestRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");

    @Mock
    private OidcAuthorizationRequestJpaRepository repository;
    @Captor
    private ArgumentCaptor<OidcAuthorizationRequestJpaEntity> entityCaptor;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OidcAuthorizationRequestRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OidcAuthorizationRequestRepositoryAdapter(repository, clock);
    }

    @Test
    void savingStoresTheRequestKeyedByItsOwnState() {
        OAuth2AuthorizationRequest request = authorizationRequest("state-123");

        adapter.saveAuthorizationRequest(request, new MockHttpServletRequest(), new MockHttpServletResponse());

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getState()).isEqualTo("state-123");
        assertThat(entityCaptor.getValue().getExpiresAt()).isAfter(NOW);
    }

    @Test
    void savingNullRemovesAnyExistingRequestForTheStateParam() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setParameter("state", "state-123");
        OidcAuthorizationRequestJpaEntity existing = entityFor("state-123", authorizationRequest("state-123"));
        when(repository.findById("state-123")).thenReturn(Optional.of(existing));

        adapter.saveAuthorizationRequest(null, httpRequest, new MockHttpServletResponse());

        verify(repository).deleteById("state-123");
    }

    @Test
    void loadingReturnsEmptyWithoutAStateParam() {
        assertThat(adapter.loadAuthorizationRequest(new MockHttpServletRequest())).isNull();
    }

    @Test
    void loadingRoundTripsAPreviouslySavedRequest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setParameter("state", "state-123");
        OAuth2AuthorizationRequest original = authorizationRequest("state-123");
        OidcAuthorizationRequestJpaEntity stored = entityFor("state-123", original);
        when(repository.findById("state-123")).thenReturn(Optional.of(stored));

        OAuth2AuthorizationRequest loaded = adapter.loadAuthorizationRequest(httpRequest);

        assertThat(loaded.getState()).isEqualTo("state-123");
        assertThat(loaded.getClientId()).isEqualTo(original.getClientId());
        assertThat(loaded.getAuthorizationRequestUri()).isEqualTo(original.getAuthorizationRequestUri());
    }

    @Test
    void loadingAnExpiredRequestReturnsNull() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setParameter("state", "state-123");
        OidcAuthorizationRequestJpaEntity expired = entityFor("state-123", authorizationRequest("state-123"));
        expired.setExpiresAt(NOW.minusSeconds(1));
        when(repository.findById("state-123")).thenReturn(Optional.of(expired));

        assertThat(adapter.loadAuthorizationRequest(httpRequest)).isNull();
    }

    @Test
    void removingDeletesAndReturnsTheStoredRequest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setParameter("state", "state-123");
        OidcAuthorizationRequestJpaEntity existing = entityFor("state-123", authorizationRequest("state-123"));
        when(repository.findById("state-123")).thenReturn(Optional.of(existing));

        OAuth2AuthorizationRequest removed = adapter.removeAuthorizationRequest(httpRequest, new MockHttpServletResponse());

        assertThat(removed.getState()).isEqualTo("state-123");
        verify(repository).deleteById("state-123");
    }

    /** Round-trips through a throwaway adapter instance to reuse its own JSON serialization. */
    private OidcAuthorizationRequestJpaEntity entityFor(String state, OAuth2AuthorizationRequest request) {
        var repo = mock(OidcAuthorizationRequestJpaRepository.class);
        var captor = ArgumentCaptor.forClass(OidcAuthorizationRequestJpaEntity.class);
        new OidcAuthorizationRequestRepositoryAdapter(repo, clock)
                .saveAuthorizationRequest(request, new MockHttpServletRequest(), new MockHttpServletResponse());
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    private static OAuth2AuthorizationRequest authorizationRequest(String state) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("google-client")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .redirectUri("http://localhost:3001/login/oauth2/code/google")
                .scopes(java.util.Set.of("openid", "profile", "email"))
                .state(state)
                .build();
    }
}
