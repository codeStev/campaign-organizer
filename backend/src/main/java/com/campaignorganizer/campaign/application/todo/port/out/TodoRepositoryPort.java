package com.campaignorganizer.campaign.application.todo.port.out;

import com.campaignorganizer.campaign.domain.todo.Todo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepositoryPort {

    /** Standing todos only (no session attached). */
    List<Todo> findStandingByCampaign(UUID campaignId);

    List<Todo> findBySession(UUID sessionId);

    /** Every todo in the campaign, standing or session-attached (interchange export). */
    List<Todo> findByCampaign(UUID campaignId);

    Optional<Todo> findByIdAndCampaign(UUID todoId, UUID campaignId);

    Todo save(Todo todo);

    void delete(Todo todo);
}
