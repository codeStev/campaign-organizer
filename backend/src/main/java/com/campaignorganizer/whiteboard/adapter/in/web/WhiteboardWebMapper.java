package com.campaignorganizer.whiteboard.adapter.in.web;

import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.EdgeDto;
import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.NodeDto;
import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.WhiteboardRequest;
import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.WhiteboardResponse;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.CreateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.UpdateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardView;
import com.campaignorganizer.whiteboard.domain.Edge;
import com.campaignorganizer.whiteboard.domain.Node;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps web DTOs ↔ commands/view (MapStruct). */
@Mapper(componentModel = "spring")
public interface WhiteboardWebMapper {

    Node toNode(NodeDto dto);

    Edge toEdge(EdgeDto dto);

    NodeDto toNodeDto(Node node);

    EdgeDto toEdgeDto(Edge edge);

    List<Node> toNodes(List<NodeDto> nodes);

    List<Edge> toEdges(List<EdgeDto> edges);

    WhiteboardResponse toResponse(WhiteboardView view);

    default CreateWhiteboardCommand toCreateCommand(UUID worldId, WhiteboardRequest request) {
        return new CreateWhiteboardCommand(worldId, request.name(),
                toNodes(request.nodes()), toEdges(request.edges()));
    }

    default UpdateWhiteboardCommand toUpdateCommand(UUID worldId, UUID whiteboardId,
                                                    WhiteboardRequest request) {
        return new UpdateWhiteboardCommand(worldId, whiteboardId, request.name(),
                toNodes(request.nodes()), toEdges(request.edges()));
    }
}
