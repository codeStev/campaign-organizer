package com.campaignorganizer.campaign.adapter.campaign.out.context;

import com.campaignorganizer.campaign.application.campaign.port.out.GameSystemExistsPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves game system existence for the campaign module via the characters context's query port. */
@Component
public class CampaignGameSystemExistsAdapter implements GameSystemExistsPort {

    private final GameSystemQueryPort systems;

    public CampaignGameSystemExistsAdapter(GameSystemQueryPort systems) {
        this.systems = systems;
    }

    @Override
    public boolean exists(UUID systemId) {
        return systems.existsById(systemId);
    }
}
