package com.campaignorganizer.whiteboard.domain;

/** A card on the plotting canvas (value object). */
public record Node(String id, String text, double x, double y, String color) {
}
