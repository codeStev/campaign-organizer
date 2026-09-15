package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit coverage for the per-IP throttle, including {@code /api/auth/recover-password}'s
 * addition to {@code THROTTLED_PATHS} alongside login/register.
 */
class RateLimitFilterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void allowsRequestsUnderTheLimitForEachThrottledPath() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(clock, 3);
        String[] paths = {"/api/auth/login", "/api/accounts/register", "/api/auth/recover-password"};

        for (int i = 0; i < paths.length; i++) {
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(requestTo(paths[i], "10.0.0." + i), response, chain);
            verify(chain, times(1)).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsRecoverPasswordRequestsOverTheLimitFromTheSameIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(clock, 2);
        String ip = "203.0.113.5";

        for (int i = 0; i < 2; i++) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(requestTo("/api/auth/recover-password", ip), new MockHttpServletResponse(), chain);
            verify(chain, times(1)).doFilter(any(), any());
        }

        FilterChain thirdChain = mock(FilterChain.class);
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        filter.doFilter(requestTo("/api/auth/recover-password", ip), thirdResponse, thirdChain);

        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        verify(thirdChain, times(0)).doFilter(any(), any());
    }

    @Test
    void neverLimitsAnUnthrottledPathRegardlessOfRequestCount() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(clock, 1);
        String ip = "203.0.113.9";

        for (int i = 0; i < 5; i++) {
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(requestTo("/api/worlds", ip), response, chain);
            verify(chain, times(1)).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private static MockHttpServletRequest requestTo(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
