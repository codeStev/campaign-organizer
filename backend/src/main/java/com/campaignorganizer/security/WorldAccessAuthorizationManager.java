package com.campaignorganizer.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * Registered in {@code SecurityConfig} against the
 * {@code /api/worlds/{worldId}} and {@code /api/worlds/{worldId}/**}
 * matchers — this one class covers every bounded context's world-nested
 * endpoints, since every one of them is path-prefixed that way (ADR-0109).
 */
@Component
public class WorldAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String WORLD_ID_VARIABLE = "worldId";

    private final WorldPermissionEvaluator evaluator;

    public WorldAccessAuthorizationManager(WorldPermissionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
                                           RequestAuthorizationContext context) {
        String worldId = context.getVariables().get(WORLD_ID_VARIABLE);
        return new AuthorizationDecision(worldId != null && isGranted(authentication.get(), worldId));
    }

    /** A malformed (non-UUID) path segment denies rather than propagating an exception. */
    private boolean isGranted(Authentication authentication, String worldId) {
        try {
            return evaluator.hasPermission(authentication, worldId, "World", WorldPermissionEvaluator.PERMISSION_ACCESS);
        } catch (IllegalArgumentException malformedWorldId) {
            return false;
        }
    }
}
