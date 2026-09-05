package com.campaignorganizer.interchange.calendar.application.port.in;

import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase.IcsCalendar;
import java.util.UUID;

/**
 * Builds an .ics calendar by subscription token (ADR-0108) — the public,
 * unauthenticated feed path a calendar app polls. Throws
 * {@link com.campaignorganizer.shared.domain.NotFoundException} for an
 * unknown token, same as any other missing resource (doesn't distinguish
 * "wrong token" from "no such campaign").
 */
public interface ExportCampaignIcsByTokenUseCase {

    IcsCalendar exportByToken(UUID token);
}
