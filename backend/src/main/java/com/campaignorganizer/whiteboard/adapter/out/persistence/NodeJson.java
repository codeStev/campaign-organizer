package com.campaignorganizer.whiteboard.adapter.out.persistence;

/** JSON-serialisable node stored in the whiteboards.nodes jsonb column. */
public record NodeJson(String id, String text, double x, double y, String color) {
}
