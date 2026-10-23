package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenQueryPort;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit coverage for API token authentication (ADR-0119): authorities, account checks, JWT passthrough. */
class ApiTokenAuthFilterTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    private final ApiTokenQueryPort apiTokens = mock(ApiTokenQueryPort.class);
    private final AccountQueryPort accounts = mock(AccountQueryPort.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final UUID accountId = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();
    private final ApiTokenAuthFilter filter = new ApiTokenAuthFilter(apiTokens, accounts);

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private ApiTokenView token(int accountTokenVersion) {
        return new ApiTokenView(tokenId, accountId, accountTokenVersion, "recorder", "cot_abcd",
                Set.of(ApiTokenScope.GLOSSARY_READ), NOW, null, null);
    }

    private AccountView account(boolean enabled, int tokenVersion) {
        return new AccountView(accountId, "gm@example.org", Role.ADMIN, enabled, tokenVersion, MfaMethod.TOTP, NOW);
    }

    private Authentication run(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void aValidTokenAuthenticatesAsItsAccountWithOnlyTheTokenFactorAndItsScopes() throws Exception {
        when(apiTokens.authenticate("cot_secret")).thenReturn(Optional.of(token(2)));
        when(accounts.findById(accountId)).thenReturn(Optional.of(account(true, 2)));

        Authentication authentication = run("Bearer cot_secret");

        assertThat(authentication.getPrincipal()).isEqualTo(accountId);
        assertThat(authentication.getDetails()).isEqualTo(tokenId);
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder(ApiTokenAuthFilter.API_TOKEN_AUTHORITY, "SCOPE_glossary:read");
    }

    @Test
    void aTokenNeverGetsTheAccountsRoleOrTheMfaFactor() throws Exception {
        when(apiTokens.authenticate("cot_secret")).thenReturn(Optional.of(token(2)));
        when(accounts.findById(accountId)).thenReturn(Optional.of(account(true, 2)));

        Authentication authentication = run("Bearer cot_secret");

        assertThat(authentication.getAuthorities()).extracting("authority")
                .doesNotContain("ROLE_ADMIN", JwtService.MFA_AUTHORITY);
    }

    @Test
    void aDisabledAccountOrAMovedOnTokenVersionInvalidatesTheToken() throws Exception {
        when(apiTokens.authenticate("cot_secret")).thenReturn(Optional.of(token(2)));
        when(accounts.findById(accountId)).thenReturn(Optional.of(account(false, 2)));
        assertThat(run("Bearer cot_secret")).isNull();

        SecurityContextHolder.clearContext();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account(true, 3)));
        assertThat(run("Bearer cot_secret")).isNull();
    }

    @Test
    void anUnknownTokenLeavesTheRequestUnauthenticated() throws Exception {
        when(apiTokens.authenticate("cot_nope")).thenReturn(Optional.empty());

        assertThat(run("Bearer cot_nope")).isNull();
    }

    @Test
    void ignoresRequestsWithoutAnApiTokenShapedBearer() throws Exception {
        assertThat(run(null)).isNull();
        assertThat(run("Bearer eyJhbGciOiJIUzI1NiJ9.e30.sig")).isNull();
        verify(apiTokens, never()).authenticate(anyString());
    }
}
