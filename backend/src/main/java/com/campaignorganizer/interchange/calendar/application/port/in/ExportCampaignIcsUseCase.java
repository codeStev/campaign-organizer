package com.campaignorganizer.interchange.calendar.application.port.in;

import java.util.UUID;

/** Builds an .ics calendar of a campaign's dated sessions (ADR-0108), authenticated path. */
public interface ExportCampaignIcsUseCase {

    IcsCalendar exportForCampaign(UUID worldId, UUID campaignId);

    /** The built calendar text plus the campaign name, for a friendly download filename. */
    record IcsCalendar(String campaignName, String icsText) {
    }
}
