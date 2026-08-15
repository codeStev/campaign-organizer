package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.CreateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;

public interface CreateRelationshipUseCase {

    RelationshipView create(CreateRelationshipCommand command);
}
