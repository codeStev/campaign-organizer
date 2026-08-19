package com.campaignorganizer.interchange.export.application.port.in;

import java.util.Map;

/** The assembled export payload: the world's name (for the filename) and the flat JSON bundle (FR-22). */
public record WorldExportBundle(String worldName, Map<String, Object> data) {
}
