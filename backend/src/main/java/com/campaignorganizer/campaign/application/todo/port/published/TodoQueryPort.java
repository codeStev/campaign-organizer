package com.campaignorganizer.campaign.application.todo.port.published;

import java.util.List;
import java.util.UUID;

/**
 * Published port: read todos from sibling aggregates and other contexts.
 * {@code findByCampaign} returns every todo in the campaign (standing and
 * session-attached) for a future per-campaign dashboard (FR-67, not yet
 * built) and for interchange export.
 */
public interface TodoQueryPort {

    List<TodoView> findByCampaign(UUID campaignId);

    List<TodoView> findBySession(UUID sessionId);
}
