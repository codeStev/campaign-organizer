package com.campaignorganizer.whiteboard.application.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.whiteboard.application.port.in.CreateWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.DeleteWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.GetWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.ListWhiteboardsUseCase;
import com.campaignorganizer.whiteboard.application.port.in.UpdateWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.CreateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.UpdateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.in.WhiteboardView;
import com.campaignorganizer.whiteboard.application.port.out.WhiteboardRepositoryPort;
import com.campaignorganizer.whiteboard.application.port.out.WorldExistsPort;
import com.campaignorganizer.whiteboard.domain.Whiteboard;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Whiteboard use cases. Owns the transaction boundary; orchestrates domain + ports. */
@Service
public class WhiteboardService
        implements CreateWhiteboardUseCase, UpdateWhiteboardUseCase, DeleteWhiteboardUseCase,
        GetWhiteboardUseCase, ListWhiteboardsUseCase {

    private final WhiteboardRepositoryPort whiteboards;
    private final WorldExistsPort worlds;
    private final WhiteboardViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public WhiteboardService(WhiteboardRepositoryPort whiteboards, WorldExistsPort worlds,
                             WhiteboardViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.whiteboards = whiteboards;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WhiteboardView create(CreateWhiteboardCommand command) {
        requireWorld(command.worldId());
        Whiteboard created = Whiteboard.create(ids.newId(), command.worldId(), command.name(),
                command.nodes(), command.edges(), clock.instant());
        return viewMapper.toView(whiteboards.save(created));
    }

    @Override
    @Transactional
    public WhiteboardView update(UpdateWhiteboardCommand command) {
        Whiteboard board = require(command.worldId(), command.whiteboardId());
        board.revise(command.name(), command.nodes(), command.edges(), clock.instant());
        return viewMapper.toView(whiteboards.save(board));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID whiteboardId) {
        whiteboards.delete(require(worldId, whiteboardId));
    }

    @Override
    @Transactional(readOnly = true)
    public WhiteboardView get(UUID worldId, UUID whiteboardId) {
        return viewMapper.toView(require(worldId, whiteboardId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhiteboardView> list(UUID worldId) {
        requireWorld(worldId);
        return whiteboards.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    private Whiteboard require(UUID worldId, UUID whiteboardId) {
        return whiteboards.findByIdAndWorld(whiteboardId, worldId)
                .orElseThrow(() -> new NotFoundException("Whiteboard not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
