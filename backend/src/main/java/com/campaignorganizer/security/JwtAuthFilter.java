package com.campaignorganizer.security;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.session.port.published.AccountSessionQueryPort;
import com.campaignorganizer.security.JwtService.ParsedToken;
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
 * Reads the {@code Authorization: Bearer <jwt>} header and, if the token is
 * well-formed and the account it names is still valid, populates the
 * security context with that account's id, role, and completed
 * authentication factors.
 *
 * "Still valid" is checked against the database on every request, not just
 * the token's own signature/expiry: an account that's been disabled, had its
 * role changed, or had its password reset/token version bumped has its
 * existing tokens invalidated immediately rather than waiting out the
 * token's natural expiry (ADR-0110) — a deliberate, small move away from
 * pure statelessness in exchange for actual revocability.
 *
 * Each entry in the token's {@code factors} claim becomes a real
 * {@link FactorGrantedAuthority} here, exactly as Spring Security's own
 * {@code AuthenticationProvider}s would grant one at login time — the
 * {@code multiFactor()} authorization rule in
 * {@code com.campaignorganizer.config.SecurityConfig} only cares that the
 * authority is present, not how it got there (ADR-0111).
 *
 * A full (PASSWORD+MFA) token additionally needs an active {@code account_sessions} row for its
 * own {@code jti} (ADR-0112) — a second, finer-grained revocation layer alongside
 * {@code tokenVersion}, letting a user revoke one device without logging out everywhere. A
 * PASSWORD-only pending-MFA token carries no such row and skips this check entirely.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;
    private final AccountQueryPort accounts;
    private final AccountSessionQueryPort sessions;

    public JwtAuthFilter(JwtService jwtService, AccountQueryPort accounts, AccountSessionQueryPort sessions) {
        this.jwtService = jwtService;
        this.accounts = accounts;
        this.sessions = sessions;
    }

    /**
     * Streaming responses (e.g. the backup download) trigger a second, internal
     * ASYNC dispatch through the filter chain to complete the response. Without
     * this override, {@link OncePerRequestFilter}'s default skips that dispatch,
     * so the security context is empty when {@code AuthorizationFilter} re-checks
     * authorization on it, and the request is denied after the response has
     * already been committed.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.parse(token).flatMap(this::toAuthentication)
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }
        filterChain.doFilter(request, response);
    }

    private Optional<UsernamePasswordAuthenticationToken> toAuthentication(ParsedToken parsed) {
        return accounts.findById(parsed.accountId())
                .filter(AccountView::enabled)
                .filter(account -> account.tokenVersion() == parsed.tokenVersion())
                .filter(account -> !parsed.factors().contains(JwtService.MFA_FACTOR) || sessions.isActive(parsed.jti()))
                .map(account -> {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            account.id(), null, authorities(account, parsed));
                    authentication.setDetails(parsed.jti());
                    return authentication;
                });
    }

    private List<GrantedAuthority> authorities(AccountView account, ParsedToken parsed) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + account.role()));
        Instant issuedAt = Instant.now();
        for (String factor : parsed.factors()) {
            authorities.add(FactorGrantedAuthority.withFactor(factor).issuedAt(issuedAt).build());
        }
        return authorities;
    }
}
