package com.campaignorganizer.accounts.application.account.service;

import com.campaignorganizer.ai.application.port.published.AiSettingsOwnershipPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateOwnershipPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldOwnershipPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Assigns every currently-unowned resource to the very first account ever created — extracted
 * out of {@code AccountService.register} so {@code OidcAccountService} (ADR-0113) can apply the
 * exact same first-registrant-becomes-ADMIN rule without duplicating five ownership-port
 * dependencies and the bootstrap call sequence itself.
 */
@Component
public class FirstAccountOwnershipBootstrapper {

    private final WorldOwnershipPort worldOwnership;
    private final AiSettingsOwnershipPort aiSettingsOwnership;
    private final GameSystemOwnershipPort gameSystemOwnership;
    private final GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership;
    private final GlobalStatblockOwnershipPort globalStatblockOwnership;

    public FirstAccountOwnershipBootstrapper(WorldOwnershipPort worldOwnership,
                                             AiSettingsOwnershipPort aiSettingsOwnership,
                                             GameSystemOwnershipPort gameSystemOwnership,
                                             GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership,
                                             GlobalStatblockOwnershipPort globalStatblockOwnership) {
        this.worldOwnership = worldOwnership;
        this.aiSettingsOwnership = aiSettingsOwnership;
        this.gameSystemOwnership = gameSystemOwnership;
        this.globalFieldTemplateOwnership = globalFieldTemplateOwnership;
        this.globalStatblockOwnership = globalStatblockOwnership;
    }

    public void assignInitialOwnership(UUID accountId) {
        worldOwnership.assignUnownedTo(accountId);
        aiSettingsOwnership.assignUnownedTo(accountId);
        gameSystemOwnership.assignUnownedTo(accountId);
        globalFieldTemplateOwnership.assignUnownedTo(accountId);
        globalStatblockOwnership.assignUnownedTo(accountId);
    }
}
