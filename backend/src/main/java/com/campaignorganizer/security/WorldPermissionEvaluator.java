package com.campaignorganizer.security;

import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The single seam where "can this account touch this resource" is decided
 * (ADR-0109). Today the rule is exactly {@code resource.ownerId ==
 * principal}; a future sharing feature would widen only this class (plus a
 * grants table), never the call sites that ask it — those already go
 * through {@code hasPermission(...)}, not the owner column directly.
 *
 * Used two ways: {@link WorldAccessAuthorizationManager} calls the id-based
 * overload for every {@code /api/worlds/{worldId}/**} request, and
 * {@code @PreAuthorize("hasPermission(...))")} on application-service
 * methods that accept a foreign catalog id in the request body (game
 * system, global field template, global statblock) resolve to this same
 * bean automatically once it's the only {@link PermissionEvaluator} bean in
 * the context.
 */
@Component
public class WorldPermissionEvaluator implements PermissionEvaluator {

    public static final String PERMISSION_ACCESS = "ACCESS";

    private static final String TARGET_WORLD = "World";
    private static final String TARGET_GAME_SYSTEM = "GameSystem";
    private static final String TARGET_GLOBAL_FIELD_TEMPLATE = "GlobalFieldTemplate";
    private static final String TARGET_GLOBAL_STATBLOCK = "GlobalStatblock";

    private final WorldQueryPort worlds;
    private final GameSystemQueryPort gameSystems;
    private final GlobalFieldTemplateQueryPort globalFieldTemplates;
    private final GlobalStatblockQueryPort globalStatblocks;

    public WorldPermissionEvaluator(WorldQueryPort worlds, GameSystemQueryPort gameSystems,
                                    GlobalFieldTemplateQueryPort globalFieldTemplates,
                                    GlobalStatblockQueryPort globalStatblocks) {
        this.worlds = worlds;
        this.gameSystems = gameSystems;
        this.globalFieldTemplates = globalFieldTemplates;
        this.globalStatblocks = globalStatblocks;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        throw new UnsupportedOperationException(
                "Only the id-based hasPermission(id, type, permission) overload is used in this app");
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        // An anonymous/unauthenticated caller's principal is Spring Security's
        // "anonymousUser" string, not an account id — deny rather than crash, and let
        // Spring's own anonymous-vs-denied handling in ExceptionTranslationFilter turn
        // that into 401 (not this evaluator's 404-masking path, which is for a real
        // account touching someone else's resource).
        if (targetId == null || !(authentication.getPrincipal() instanceof UUID accountId)) {
            return false;
        }
        UUID resourceId = targetId instanceof UUID uuid ? uuid : UUID.fromString(targetId.toString());
        return ownerOf(targetType, resourceId).map(accountId::equals).orElse(false);
    }

    private Optional<UUID> ownerOf(String targetType, UUID resourceId) {
        return switch (targetType) {
            case TARGET_WORLD -> worlds.findById(resourceId).map(WorldView::ownerId);
            case TARGET_GAME_SYSTEM -> gameSystems.findById(resourceId).map(GameSystemView::ownerId);
            case TARGET_GLOBAL_FIELD_TEMPLATE ->
                    globalFieldTemplates.findById(resourceId).map(GlobalFieldTemplateView::ownerId);
            case TARGET_GLOBAL_STATBLOCK -> globalStatblocks.findById(resourceId).map(GlobalStatblockView::ownerId);
            default -> throw new IllegalArgumentException("Unknown permission target type: " + targetType);
        };
    }
}
