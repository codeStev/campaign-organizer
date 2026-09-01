package com.campaignorganizer.campaign.application.session.port.published;

/** Published import for backup/restore (ADR-0061): saves an attendance row verbatim. */
public interface SessionAttendanceImportPort {

    SessionAttendanceView importAttendance(SessionAttendanceView view);
}
