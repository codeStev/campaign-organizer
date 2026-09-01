package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.SetCampaignRosterCommand;
import java.util.List;

public interface SetCampaignRosterUseCase {

    List<RosterEntry> set(SetCampaignRosterCommand command);
}
