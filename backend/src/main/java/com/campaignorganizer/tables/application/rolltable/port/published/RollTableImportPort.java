package com.campaignorganizer.tables.application.rolltable.port.published;

/** Published import for backup/restore (ADR-0061): saves a table verbatim. */
public interface RollTableImportPort {

    RollTableView importRollTable(RollTableView view);
}
