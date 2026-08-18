package com.campaignorganizer.worldbuilding.application.relationship.service;

import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import com.campaignorganizer.worldbuilding.domain.relationship.Relationship;
import org.mapstruct.Mapper;

/** Maps the domain relationship to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface RelationshipViewMapper {

    RelationshipView toView(Relationship relationship);
}
