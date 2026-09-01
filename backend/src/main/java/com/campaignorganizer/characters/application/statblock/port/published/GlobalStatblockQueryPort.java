package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.List;

/** Published port: read the global statblock catalog from sibling contexts (ADR-0096). */
public interface GlobalStatblockQueryPort {

    List<GlobalStatblockView> findAll();
}
