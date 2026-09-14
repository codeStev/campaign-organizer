package com.campaignorganizer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP throttle on the two unauthenticated, publicly-reachable endpoints
 * (login, registration) — the per-account lockout in {@code AccountService}
 * stops someone hammering *one* account, but not someone sweeping many
 * emails, or forcing repeated BCrypt hashing (a real CPU-cost DoS vector
 * once this is reachable from the open internet). Deliberately dependency-
 * free (a fixed-window counter, not a proper token bucket) rather than
 * pulling in a rate-limiting library for two endpoints; revisit if more
 * endpoints need this.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> THROTTLED_PATHS = Set.of("/api/auth/login", "/api/accounts/register");
    private static final Duration WINDOW = Duration.ofMinutes(1);
    /** Not in {@link HttpServletResponse} — 429 predates the constants it defines. */
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final Clock clock;
    private final int maxRequestsPerWindow;
    private final ConcurrentHashMap<String, Window> windowsByIp = new ConcurrentHashMap<>();

    public RateLimitFilter(Clock clock,
                           @Value("${app.rate-limit.max-requests-per-minute:20}") int maxRequestsPerWindow) {
        this.clock = clock;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (THROTTLED_PATHS.contains(request.getRequestURI()) && isRateLimited(clientIp(request))) {
            response.setStatus(SC_TOO_MANY_REQUESTS);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        Instant now = clock.instant();
        Window window = windowsByIp.compute(ip, (key, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new Window(now.plus(WINDOW));
            }
            return existing;
        });
        return window.count.incrementAndGet() > maxRequestsPerWindow;
    }

    /** {@code X-Forwarded-For} first, since a reverse proxy sits in front of this app in every real deployment. */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final Instant expiresAt;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
