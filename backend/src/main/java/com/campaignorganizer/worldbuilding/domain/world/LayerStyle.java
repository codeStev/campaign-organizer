package com.campaignorganizer.worldbuilding.domain.world;

/** Styling for a map layer (by layer name): a colour and/or an icon key (ADR-0049). */
public record LayerStyle(String color, String icon) {
}
