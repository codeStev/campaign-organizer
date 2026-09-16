package com.campaignorganizer.accounts.application.account.service;

import com.campaignorganizer.ai.application.port.published.AiSettingsOwnershipPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateOwnershipPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldOwnershipPort;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Assigns every currently-unowned resource to the very first account ever created — extracted
 * out of {@code AccountService.register} so {@code OidcAccountService} (ADR-0113) can apply the
 * exact same first-registrant-becomes-ADMIN rule without duplicating five ownership-port
 * dependencies and the bootstrap call sequence itself.
 *
 * <p>The only codepath that legitimately needs to see every account's unowned rows at once
 * (ADR-0114) — no account is authenticated yet at registration time, so there's no GUC to key
 * a normal RLS policy off in the first place. {@code SET LOCAL ROLE app_rls_bypass} switches
 * the current transaction (the same one, same connection, as the new account's own {@code
 * INSERT} — see the {@code flushAutomatically} comment on each {@code assignUnownedTo} query)
 * to a role with {@code BYPASSRLS}, reverting automatically at transaction end.
 */
@Component
public class FirstAccountOwnershipBootstrapper {

    private final EntityManager entityManager;
    private final WorldOwnershipPort worldOwnership;
    private final AiSettingsOwnershipPort aiSettingsOwnership;
    private final GameSystemOwnershipPort gameSystemOwnership;
    private final GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership;
    private final GlobalStatblockOwnershipPort globalStatblockOwnership;

    public FirstAccountOwnershipBootstrapper(EntityManager entityManager, WorldOwnershipPort worldOwnership,
                                             AiSettingsOwnershipPort aiSettingsOwnership,
                                             GameSystemOwnershipPort gameSystemOwnership,
                                             GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership,
                                             GlobalStatblockOwnershipPort globalStatblockOwnership) {
        this.entityManager = entityManager;
        this.worldOwnership = worldOwnership;
        this.aiSettingsOwnership = aiSettingsOwnership;
        this.gameSystemOwnership = gameSystemOwnership;
        this.globalFieldTemplateOwnership = globalFieldTemplateOwnership;
        this.globalStatblockOwnership = globalStatblockOwnership;
    }

    public void assignInitialOwnership(UUID accountId) {
        entityManager.createNativeQuery("SET LOCAL ROLE app_rls_bypass").executeUpdate();
        worldOwnership.assignUnownedTo(accountId);
        aiSettingsOwnership.assignUnownedTo(accountId);
        gameSystemOwnership.assignUnownedTo(accountId);
        globalFieldTemplateOwnership.assignUnownedTo(accountId);
        globalStatblockOwnership.assignUnownedTo(accountId);
    }
}
