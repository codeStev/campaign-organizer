package com.campaignorganizer.tables.application.rolltable.port.in;

import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.UpdateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;

public interface UpdateRollTableUseCase {

    RollTableView update(UpdateRollTableCommand command);
}
