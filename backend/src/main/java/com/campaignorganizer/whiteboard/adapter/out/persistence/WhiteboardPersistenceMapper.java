package com.campaignorganizer.whiteboard.adapter.out.persistence;

import com.campaignorganizer.whiteboard.domain.Edge;
import com.campaignorganizer.whiteboard.domain.Node;
import com.campaignorganizer.whiteboard.domain.Whiteboard;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface WhiteboardPersistenceMapper {

    WhiteboardJpaEntity toEntity(Whiteboard whiteboard);

    NodeJson toJson(Node node);

    EdgeJson toJson(Edge edge);

    Node toNode(NodeJson json);

    Edge toEdge(EdgeJson json);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default Whiteboard toDomain(WhiteboardJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Whiteboard.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getName(),
                entity.getNodes().stream().map(this::toNode).toList(),
                entity.getEdges().stream().map(this::toEdge).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
