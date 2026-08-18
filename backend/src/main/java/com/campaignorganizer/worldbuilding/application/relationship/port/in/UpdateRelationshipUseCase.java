package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.UpdateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;

public interface UpdateRelationshipUseCase {

    RelationshipView update(UpdateRelationshipCommand command);
}
