package com.campaignorganizer.tables.application.rolltable.service;

import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.tables.domain.rolltable.RollTable;
import com.campaignorganizer.tables.domain.rolltable.RollTableEntry;
import org.mapstruct.Mapper;

/** Maps the domain roll table to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface RollTableViewMapper {

    RollTableView toView(RollTable table);

    RollTableEntryView toEntryView(RollTableEntry entry);
}
