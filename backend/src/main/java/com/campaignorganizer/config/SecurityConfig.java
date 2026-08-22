package com.campaignorganizer.config;

import com.campaignorganizer.security.JwtAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security. The only public endpoints are the login route and the
 * generated API docs; everything under /api/** otherwise requires a valid
 * owner bearer token. Everything *outside* /api/** is left open at this
 * layer too — on the combined image (ADR-0059) that's the static SPA shell,
 * which must load before the user can log in; on the API-only image there's
 * nothing there to serve, so this is a no-op. See
 * docs/adr/0006-single-password-auth.md.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        // Public image serving, addressed by unguessable id (ADR-0016).
                        .requestMatchers(HttpMethod.GET, "/api/media/*/content").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
