package com.campaignorganizer.campaign.application.clock.port.published;

/** Published read model for one clock segment. */
public record ClockSegmentView(boolean filled, String title, String description) {
}
