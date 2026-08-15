package com.campaignorganizer.whiteboard.adapter.out.persistence;

/** JSON-serialisable edge stored in the whiteboards.edges jsonb column. */
public record EdgeJson(String id, String fromNodeId, String toNodeId, String label) {
}
