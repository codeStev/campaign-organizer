package com.campaignorganizer.tables.application.rolltable.port.in;

import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import java.util.List;
import java.util.UUID;

public interface ListRollTablesUseCase {

    List<RollTableView> list(UUID worldId);
}
