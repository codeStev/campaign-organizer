package com.campaignorganizer.tables.application.rolltable.port.in;

import java.util.UUID;

public interface DeleteRollTableUseCase {

    void delete(UUID worldId, UUID tableId);
}
