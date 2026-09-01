package com.campaignorganizer.tables.application.rolltable.port.in;

import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import java.util.UUID;

public interface DuplicateRollTableUseCase {

    RollTableView duplicate(UUID worldId, UUID tableId);
}
