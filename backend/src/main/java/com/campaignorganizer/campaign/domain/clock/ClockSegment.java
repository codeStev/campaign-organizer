package com.campaignorganizer.campaign.domain.clock;

/**
 * One wedge of a clock (value object, no own id). Most segments are unlabeled
 * generic progress; only narratively meaningful ones carry a title/description
 * (ADR-0084).
 */
public record ClockSegment(boolean filled, String title, String description) {
}
