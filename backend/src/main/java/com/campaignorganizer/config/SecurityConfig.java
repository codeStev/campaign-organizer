package com.campaignorganizer.config;

import com.campaignorganizer.security.JwtAuthFilter;
import com.campaignorganizer.security.ProblemDetailAccessDeniedHandler;
import com.campaignorganizer.security.RateLimitFilter;
import com.campaignorganizer.security.WorldAccessAuthorizationManager;
import com.campaignorganizer.security.WorldPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Stateless security. The only public endpoints are login, registration, and
 * the generated API docs; everything else under /api/** requires a valid
 * account bearer token — and every /api/worlds/{worldId}/** endpoint
 * additionally requires that token's account to own that World (ADR-0109),
 * enforced by {@link WorldAccessAuthorizationManager} rather than a
 * per-controller check. Everything *outside* /api/** is left open at this
 * layer too — on the combined image (ADR-0059) that's the static SPA shell,
 * which must load before the user can log in; on the API-only image there's
 * nothing there to serve, so this is a no-op. See
 * docs/adr/0110-self-registration-and-role-based-jwt.md.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/login",
            "/api/accounts/register",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           RateLimitFilter rateLimitFilter,
                                           WorldAccessAuthorizationManager worldAccessAuthorizationManager)
            throws Exception {
        // Same gotcha as the /api/worlds/{worldId} matcher below: "/**" alone doesn't reliably
        // match the bare "/api/accounts" resource itself (no trailing segment), so both forms
        // are needed.
        RequestMatcher accountsMatcher = new OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern("/api/accounts"),
                PathPatternRequestMatcher.pathPattern("/api/accounts/**"));
        // A plain ObjectMapper, not the application's autoconfigured bean: these handlers only
        // ever serialize a plain ProblemDetail (status/title/detail strings), so they don't need
        // whatever modules/customizations the app-wide mapper carries, and staying independent of
        // it avoids tying filter-chain construction to Jackson autoconfiguration having already run.
        ObjectMapper objectMapper = new ObjectMapper();
        // The default: every hasPermission(...)-driven ownership denial — whether from
        // WorldAccessAuthorizationManager on a /api/worlds/** request, or from a
        // @PreAuthorize("hasPermission(...))") check on a standalone catalog service
        // (game systems, global templates, global statblocks) — masks as 404, identical
        // to a nonexistent id, so a stranger's resource id doesn't confirm it exists
        // (ADR-0109). Scoped narrower: hasRole('ADMIN') denials on account management
        // are a different, non-sensitive kind of denial and get a real 403.
        AccessDeniedHandler notFoundOnOwnershipMismatch = new ProblemDetailAccessDeniedHandler(
                HttpStatus.NOT_FOUND, "Not found", "The requested resource does not exist.", objectMapper);
        AccessDeniedHandler forbiddenOnRoleMismatch = new ProblemDetailAccessDeniedHandler(
                HttpStatus.FORBIDDEN, "Forbidden",
                "Your account's role does not permit this action.", objectMapper);
        // NOT Spring's own .defaultAccessDeniedHandlerFor(...) + .accessDeniedHandler(...) pair:
        // those don't compose the way their names suggest — an explicit .accessDeniedHandler(...)
        // is returned unconditionally and Spring never consults the per-matcher mapping at all, so
        // every denial silently got the "default" handler regardless of path. Routing by hand here
        // instead, verified against a live run (curl against a real ADMIN-only endpoint as a USER).
        AccessDeniedHandler routedByPath = (request, response, ex) -> {
            if (accountsMatcher.matches(request)) {
                forbiddenOnRoleMismatch.handle(request, response, ex);
            } else {
                notFoundOnOwnershipMismatch.handle(request, response, ex);
            }
        };
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        // Public image serving, addressed by unguessable id (ADR-0016).
                        .requestMatchers(HttpMethod.GET, "/api/media/*/content").permitAll()
                        // Public .ics subscription feed, addressed by unguessable token (ADR-0108).
                        .requestMatchers(HttpMethod.GET, "/api/calendar/*.ics").permitAll()
                        // Named {worldId} template variable, not a plain wildcard: WorldAccessAuthorizationManager
                        // reads it back out of RequestAuthorizationContext.getVariables(). Both forms are needed —
                        // "/**" alone doesn't reliably match the bare "/api/worlds/{worldId}" resource itself.
                        .requestMatchers("/api/worlds/{worldId}", "/api/worlds/{worldId}/**")
                        .access(worldAccessAuthorizationManager)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(routedByPath))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }

    /**
     * Wires {@link WorldPermissionEvaluator} into {@code @PreAuthorize("hasPermission(...))")}
     * expressions (used for the cross-reference ownership checks in {@code campaign}/{@code characters}
     * services) — {@code @EnableMethodSecurity} doesn't auto-detect a {@code PermissionEvaluator} bean.
     */
    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            WorldPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
