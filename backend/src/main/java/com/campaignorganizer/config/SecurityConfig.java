package com.campaignorganizer.config;

import com.campaignorganizer.accounts.adapter.account.out.mfa.WebAuthnAuthenticationSuccessHandler;
import com.campaignorganizer.accounts.adapter.account.out.oidc.OidcAuthenticationFailureHandler;
import com.campaignorganizer.accounts.adapter.account.out.oidc.OidcAuthenticationSuccessHandler;
import com.campaignorganizer.accounts.adapter.account.out.persistence.OidcAuthorizationRequestRepositoryAdapter;
import com.campaignorganizer.accounts.adapter.account.out.persistence.WebAuthnCreationOptionsRepositoryAdapter;
import com.campaignorganizer.accounts.adapter.account.out.persistence.WebAuthnRequestOptionsRepositoryAdapter;
import com.campaignorganizer.security.JwtAuthFilter;
import com.campaignorganizer.security.JwtService;
import com.campaignorganizer.security.ProblemDetailAccessDeniedHandler;
import com.campaignorganizer.security.RateLimitFilter;
import com.campaignorganizer.security.WorldAccessAuthorizationManager;
import com.campaignorganizer.security.WorldPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;

/**
 * Stateless security. The only public endpoints are login, registration, the
 * password-recovery endpoint, and the generated API docs. Every other
 * /api/** request requires a valid account bearer token carrying both the
 * PASSWORD and MFA authentication factors (ADR-0111, mandatory MFA, no
 * opt-out) — enforced by a {@code AuthorizationManagerFactories.multiFactor()}
 * rule built from Spring Security 7's own {@link FactorGrantedAuthority}
 * primitives, the same authorities {@link JwtAuthFilter} grants from a
 * token's {@code factors} claim. The {@code /api/auth/mfa/**} endpoints are
 * the one exception: they only need the PASSWORD factor, since their whole
 * purpose is completing the MFA factor in the first place. Every
 * /api/worlds/{worldId}/** endpoint additionally requires that token's
 * account to own that World (ADR-0109), enforced by
 * {@link WorldAccessAuthorizationManager} — combined with the MFA
 * requirement via {@link AuthorizationManagers#allOf}, since
 * {@code authorizeHttpRequests} only evaluates the first matching rule for
 * a given path, not every rule that happens to match. Everything *outside*
 * /api/** is left open at this layer too — on the combined image (ADR-0059)
 * that's the static SPA shell, which must load before the user can log in;
 * on the API-only image there's nothing there to serve, so this is a no-op.
 * The MFA factor can be completed either via this app's own TOTP endpoints
 * under {@code /api/auth/mfa/**}, or via Spring Security's own {@code
 * .webAuthn()} DSL, wired up below at {@code /webauthn/**} and {@code
 * /login/webauthn} — CSRF protection is scoped to just those paths, since
 * the rest of this API is a pure bearer-token service with no cookies.
 * Google sign-in (ADR-0113) is a third, independent way to reach the
 * PASSWORD factor (not a bypass of the MFA factor above) — registered via
 * {@code .oauth2Login()} only when actually configured; see {@link
 * AppProperties.Oidc}.
 * See docs/adr/0110-self-registration-and-role-based-jwt.md,
 * docs/adr/0111-mandatory-mfa.md, and docs/adr/0113-sso-oidc-login.md.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/login",
            "/api/auth/recover-password",
            "/api/auth/oidc/status",
            "/api/auth/oidc/exchange",
            "/api/accounts/register",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    /**
     * Google's own login-initiation/callback endpoints (ADR-0113) — plain GET browser
     * navigations, always public (there's no account/token yet at this point in the flow). No
     * CSRF matcher entry needed, unlike {@code /webauthn/**}: {@code CsrfFilter} only protects
     * unsafe methods by default, and the OAuth {@code state} parameter is what protects the
     * callback instead.
     */
    private static final String[] OIDC_PATHS = {
            "/oauth2/**",
            "/login/oauth2/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           RateLimitFilter rateLimitFilter,
                                           WorldAccessAuthorizationManager worldAccessAuthorizationManager,
                                           AppProperties properties,
                                           WebAuthnCreationOptionsRepositoryAdapter webAuthnCreationOptionsRepository,
                                           WebAuthnRequestOptionsRepositoryAdapter webAuthnRequestOptionsRepository,
                                           WebAuthnAuthenticationSuccessHandler webAuthnAuthenticationSuccessHandler,
                                           OidcAuthorizationRequestRepositoryAdapter oidcAuthorizationRequestRepository,
                                           OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler,
                                           OidcAuthenticationFailureHandler oidcAuthenticationFailureHandler)
            throws Exception {
        // @EnableMethodSecurity's session/redirect-oriented multi-factor annotation
        // (@EnableMultiFactorAuthentication) isn't used here — it assumes chained
        // formLogin()/webAuthn() logins with automatic redirects, which doesn't fit this
        // app's stateless JSON API. Instead, JwtAuthFilter grants FactorGrantedAuthority
        // instances by hand from a token's `factors` claim, and this plain
        // AuthorizationManagerFactories.multiFactor() rule — usable exactly like any other
        // custom AuthorizationManager via .access(...) below — requires both of them.
        AuthorizationManagerFactory<RequestAuthorizationContext> mfaFactory =
                AuthorizationManagerFactories.<RequestAuthorizationContext>multiFactor()
                        .requireFactors(FactorGrantedAuthority.PASSWORD_AUTHORITY, JwtService.MFA_AUTHORITY)
                        .build();
        AuthorizationManager<RequestAuthorizationContext> mfaRequired = mfaFactory.authenticated();
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
        // A token missing the MFA factor (ADR-0111) — either never finished enrollment, or
        // hasn't cleared this session's challenge yet — denies every AuthorizationManager built
        // above the same way an ownership/role mismatch does, so it needs its own branch here:
        // otherwise it would silently fall into the 404 ownership-mismatch case, which is
        // confusing (the resource exists; the caller just isn't fully authenticated yet).
        AccessDeniedHandler mfaRequiredHandler = new ProblemDetailAccessDeniedHandler(
                HttpStatus.FORBIDDEN, "MFA required",
                "Complete multi-factor authentication via /api/auth/mfa/** before retrying.", objectMapper);
        // CsrfFilter runs ahead of JwtAuthFilter in the chain, so a CSRF failure on /webauthn/**
        // reaches this same accessDeniedHandler with SecurityContextHolder still empty — without
        // this branch it fell through to the ownership-mismatch 404 below, which is a misleading
        // response for "you forgot the X-XSRF-TOKEN header" (caught by WebAuthnCeremonyWiringIT).
        AccessDeniedHandler invalidCsrfTokenHandler = new ProblemDetailAccessDeniedHandler(
                HttpStatus.FORBIDDEN, "Invalid CSRF token",
                "Missing or invalid X-XSRF-TOKEN header for this request.", objectMapper);
        // NOT Spring's own .defaultAccessDeniedHandlerFor(...) + .accessDeniedHandler(...) pair:
        // those don't compose the way their names suggest — an explicit .accessDeniedHandler(...)
        // is returned unconditionally and Spring never consults the per-matcher mapping at all, so
        // every denial silently got the "default" handler regardless of path. Routing by hand here
        // instead, verified against a live run (curl against a real ADMIN-only endpoint as a USER).
        AccessDeniedHandler routedByPath = (request, response, ex) -> {
            if (ex instanceof CsrfException) {
                invalidCsrfTokenHandler.handle(request, response, ex);
                return;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean missingMfaFactor = authentication != null && authentication.getAuthorities().stream()
                    .noneMatch(authority -> JwtService.MFA_AUTHORITY.equals(authority.getAuthority()));
            if (missingMfaFactor) {
                mfaRequiredHandler.handle(request, response, ex);
            } else if (accountsMatcher.matches(request)) {
                forbiddenOnRoleMismatch.handle(request, response, ex);
            } else {
                notFoundOnOwnershipMismatch.handle(request, response, ex);
            }
        };
        // Spring's WebAuthn endpoints (/webauthn/**, /login/webauthn) expect a CSRF token —
        // this app otherwise disables CSRF entirely, since it's a pure bearer-token API with no
        // cookies anywhere else. requireCsrfProtectionMatcher scopes *enforcement* to just those
        // paths; every other request is completely unaffected (CsrfFilter still runs but never
        // requires a token). withHttpOnlyFalse(): the SPA's own JS needs to read this cookie to
        // echo it back as a header (the standard double-submit pattern), not an admission that
        // anything else about this token is sensitive.
        RequestMatcher webAuthnPaths = new OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern("/webauthn/**"),
                PathPatternRequestMatcher.pathPattern("/login/webauthn"));
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(webAuthnPaths)
                        // Without this, CookieCsrfTokenRepository's token is only generated (and
                        // its cookie only written to the response) once something resolves the
                        // deferred CsrfToken — the SPA would have no way to get its first cookie
                        // before making its first protected WebAuthn request. .spa() swaps in a
                        // request handler that resolves it eagerly on every request instead.
                        .spa())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // rpName defaults to rpId if blank, matching WebAuthnConfigurer's own fallback.
                // disableDefaultRegistrationPage: this app has its own React frontend — Spring's
                // auto-generated HTML registration/login pages (and their supporting filters)
                // would otherwise be registered for no reason. creationOptionsRepository is the
                // *only* DSL hook WebAuthnConfigurer exposes for a stateless deployment — the
                // authentication-side equivalent has none (confirmed by reading its source), so
                // that one is wired by hand below, after http.build().
                .webAuthn(webAuthn -> webAuthn
                        .rpId(properties.webauthn().relyingPartyId())
                        .rpName(properties.webauthn().relyingPartyName())
                        .allowedOrigins(properties.webauthn().allowedOrigins())
                        .disableDefaultRegistrationPage(true)
                        .creationOptionsRepository(webAuthnCreationOptionsRepository));
        // Google sign-in (ADR-0113) — registered only when actually configured, so a deployment
        // with no Google credentials never gets /oauth2/authorization/google at all rather than
        // an unusable, half-wired endpoint. Unlike WebAuthn's login side, oauth2Login()'s DSL
        // does expose a direct hook for swapping in a stateless AuthorizationRequestRepository —
        // no post-http.build() filter-searching needed here. The ClientRegistration is built by
        // hand and passed directly (not read from a Spring-managed ClientRegistrationRepository
        // bean) — see googleClientRegistrationRepository's Javadoc for why.
        if (properties.oidc().googleEnabled()) {
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(googleClientRegistrationRepository(properties.oidc()))
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(oidcAuthorizationRequestRepository))
                    .successHandler(oidcAuthenticationSuccessHandler)
                    .failureHandler(oidcAuthenticationFailureHandler));
        }
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        .requestMatchers(OIDC_PATHS).permitAll()
                        // Public image serving, addressed by unguessable id (ADR-0016).
                        .requestMatchers(HttpMethod.GET, "/api/media/*/content").permitAll()
                        // Public .ics subscription feed, addressed by unguessable token (ADR-0108).
                        .requestMatchers(HttpMethod.GET, "/api/calendar/*.ics").permitAll()
                        // Only the PASSWORD factor is required here — completing MFA is exactly what
                        // these endpoints are for, so requiring the MFA factor already would be circular.
                        // Same reasoning for Spring's own WebAuthn ceremony endpoints below — though
                        // note this rule only actually reaches the credential-writing /webauthn/register
                        // (WebAuthnRegistrationFilter is wired addFilterAfter(AuthorizationFilter): this
                        // rule is what protects it). /webauthn/register/options, /webauthn/authenticate/
                        // options, and /login/webauthn each self-terminate the chain before reaching
                        // AuthorizationFilter (confirmed by reading their source), so this line is
                        // vacuous for those three — register/options has its own internal "any
                        // authenticated principal" check instead, and authenticate/options + login are
                        // deliberately reachable pre-auth (a resident-key/passkey login can't know who's
                        // signing in before a credential is picked). CSRF (below) is what actually gates
                        // all four paths uniformly.
                        .requestMatchers("/api/auth/mfa/**").authenticated()
                        .requestMatchers("/webauthn/**", "/login/webauthn").authenticated()
                        // Named {worldId} template variable, not a plain wildcard: WorldAccessAuthorizationManager
                        // reads it back out of RequestAuthorizationContext.getVariables(). Both forms are needed —
                        // "/**" alone doesn't reliably match the bare "/api/worlds/{worldId}" resource itself.
                        .requestMatchers("/api/worlds/{worldId}", "/api/worlds/{worldId}/**")
                        .access(AuthorizationManagers.allOf(mfaRequired, worldAccessAuthorizationManager))
                        .requestMatchers("/api/**").access(mfaRequired)
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(routedByPath))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
        SecurityFilterChain chain = http.build();
        // WebAuthnConfigurer exposes no DSL hook for the authentication (login-challenge) side's
        // PublicKeyCredentialRequestOptionsRepository, or for WebAuthnAuthenticationFilter's
        // success handler / session-backed SecurityContextRepository — confirmed by reading its
        // actual source rather than assuming a hook exists. Both filter instances are still
        // ordinary objects with public setters once the chain is built, so this is a direct,
        // supported way to finish configuring them — not a workaround.
        for (Filter filter : chain.getFilters()) {
            if (filter instanceof WebAuthnAuthenticationFilter webAuthnAuthFilter) {
                webAuthnAuthFilter.setRequestOptionsRepository(webAuthnRequestOptionsRepository);
                webAuthnAuthFilter.setSecurityContextRepository(new RequestAttributeSecurityContextRepository());
                webAuthnAuthFilter.setAuthenticationSuccessHandler(webAuthnAuthenticationSuccessHandler);
            } else if (filter instanceof PublicKeyCredentialRequestOptionsFilter requestOptionsFilter) {
                requestOptionsFilter.setRequestOptionsRepository(webAuthnRequestOptionsRepository);
            }
        }
        return chain;
    }

    /**
     * Builds Google's {@link ClientRegistration} by hand rather than relying on Spring Boot's
     * own {@code spring.security.oauth2.client.registration.*} autoconfiguration (ADR-0113).
     * That autoconfiguration validates every configured registration <b>eagerly, at
     * bean-creation time</b> — {@code OAuth2ClientPropertiesMapper} throws {@code
     * IllegalStateException("Client id of registration 'google' must not be empty")} the moment
     * any properties exist under that prefix with a blank client id, which would break
     * <i>every</i> request in this app (a failed bean fails the whole context) on any deployment
     * that hasn't configured Google — not the "optional, silently skipped" behavior every other
     * external credential in this app has (AI provider keys, and now this). Only called when
     * {@link AppProperties.Oidc#googleEnabled()} is already true, so {@code clientId}/{@code
     * clientSecret} here are real. Google's endpoints are stable, publicly documented, and
     * unlikely to change — hardcoding them avoids an extra live discovery HTTP call
     * ({@code ClientRegistrations.fromIssuerLocation(...)}) at every app startup.
     */
    private ClientRegistrationRepository googleClientRegistrationRepository(AppProperties.Oidc oidc) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(oidc.googleClientId())
                .clientSecret(oidc.googleClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
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
