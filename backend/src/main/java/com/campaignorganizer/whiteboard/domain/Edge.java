package com.campaignorganizer.whiteboard.domain;

/** A connector between two nodes (value object). */
public record Edge(String id, String fromNodeId, String toNodeId, String label) {
}
