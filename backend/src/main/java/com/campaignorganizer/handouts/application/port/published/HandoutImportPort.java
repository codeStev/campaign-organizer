package com.campaignorganizer.handouts.application.port.published;

/** Published import for backup/restore (ADR-0061): saves a handout verbatim. */
public interface HandoutImportPort {

    HandoutView importHandout(HandoutView view);
}
