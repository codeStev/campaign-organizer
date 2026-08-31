package com.campaignorganizer.campaign.adapter.clock.out.persistence;

/** JSON-serialisable segment stored in the clocks.segments jsonb column. */
public record ClockSegmentJson(boolean filled, String title, String description) {
}
