package com.campaignorganizer.tables.application.rolltable.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
