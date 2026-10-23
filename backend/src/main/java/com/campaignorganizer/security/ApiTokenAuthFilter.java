package com.campaignorganizer.security;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenQueryPort;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates {@code Authorization: Bearer cot_...} API tokens (ADR-0119). The principal is the
 * owning account's id, exactly as for a JWT login, so ownership checks and row-level security
 * behave identically. The authorities are deliberately minimal: an {@link #API_TOKEN_AUTHORITY}
 * factor and one {@code SCOPE_<wire value>} per scope. No role and no MFA factor, so a token never
 * satisfies {@code mfaRequired}; it only passes rules that explicitly name one of its scopes.
 *
 * A token also stops working when its account is disabled or the account's {@code token_version}
 * has moved on since the token was created (password change, logout-all, MFA reset, role change).
 */
@Component
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    public static final String API_TOKEN_FACTOR = "API_TOKEN";
    public static final String API_TOKEN_AUTHORITY = "FACTOR_" + API_TOKEN_FACTOR;
    public static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX = "cot_";

    private final ApiTokenQueryPort apiTokens;
    private final AccountQueryPort accounts;

    public ApiTokenAuthFilter(ApiTokenQueryPort apiTokens, AccountQueryPort accounts) {
        this.apiTokens = apiTokens;
        this.accounts = accounts;
    }

    public static String scopeAuthority(ApiTokenScope scope) {
        return SCOPE_AUTHORITY_PREFIX + scope.wireValue();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX + TOKEN_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String secret = header.substring(BEARER_PREFIX.length());
            apiTokens.authenticate(secret).flatMap(this::toAuthentication)
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }
        filterChain.doFilter(request, response);
    }

    private Optional<UsernamePasswordAuthenticationToken> toAuthentication(ApiTokenView token) {
        return accounts.findById(token.accountId())
                .filter(AccountView::enabled)
                .filter(account -> account.tokenVersion() == token.accountTokenVersion())
                .map(account -> {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            account.id(), null, authorities(token));
                    authentication.setDetails(token.id());
                    return authentication;
                });
    }

    private List<GrantedAuthority> authorities(ApiTokenView token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(FactorGrantedAuthority.withFactor(API_TOKEN_FACTOR).issuedAt(Instant.now()).build());
        token.scopes().forEach(scope -> authorities.add(new SimpleGrantedAuthority(scopeAuthority(scope))));
        return authorities;
    }
}
