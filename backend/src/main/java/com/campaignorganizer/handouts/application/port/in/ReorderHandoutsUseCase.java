package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.in.HandoutCommands.ReorderHandoutsCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import java.util.List;

public interface ReorderHandoutsUseCase {

    List<HandoutView> reorder(ReorderHandoutsCommand command);
}
