package com.campaignorganizer.whiteboard;

/** JSON-serializable canvas primitives (see ADR-0027). */
public final class WhiteboardSchema {

    private WhiteboardSchema() {
    }

    public record Node(String id, String text, double x, double y, String color) {
    }

    public record Edge(String id, String fromNodeId, String toNodeId, String label) {
    }
}
