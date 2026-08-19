package com.campaignorganizer.interchange.export.application.port.in;

import java.util.UUID;

public interface ExportWorldUseCase {

    WorldExportBundle export(UUID worldId);
}
