package com.campaignorganizer.tables.application.rolltable.port.in;

import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.CreateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;

public interface CreateRollTableUseCase {

    RollTableView create(CreateRollTableCommand command);
}
