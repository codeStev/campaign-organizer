package com.campaignorganizer.handouts.application.service;

import com.campaignorganizer.handouts.application.port.in.CreateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.GetHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.CreateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.ReorderHandoutsCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.UpdateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.ListHandoutsUseCase;
import com.campaignorganizer.handouts.application.port.in.ReorderHandoutsUseCase;
import com.campaignorganizer.handouts.application.port.in.UpdateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.out.HandoutRepositoryPort;
import com.campaignorganizer.handouts.application.port.out.SessionExistsPort;
import com.campaignorganizer.handouts.application.port.out.WorldExistsPort;
import com.campaignorganizer.handouts.application.port.published.HandoutImportPort;
import com.campaignorganizer.handouts.application.port.published.HandoutQueryPort;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.handouts.domain.Handout;
import com.campaignorganizer.handouts.domain.Handout.Preset;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handout use cases; also implements the published query/import ports. */
@Service
public class HandoutService implements CreateHandoutUseCase, UpdateHandoutUseCase,
        DeleteHandoutUseCase, ListHandoutsUseCase, GetHandoutUseCase, ReorderHandoutsUseCase,
        HandoutQueryPort, HandoutImportPort {

    private final HandoutRepositoryPort handouts;
    private final WorldExistsPort worlds;
    private final SessionExistsPort sessions;
    private final HandoutViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public HandoutService(HandoutRepositoryPort handouts, WorldExistsPort worlds,
                          SessionExistsPort sessions, HandoutViewMapper viewMapper,
                          IdGenerator ids, Clock clock) {
        this.handouts = handouts;
        this.worlds = worlds;
        this.sessions = sessions;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HandoutView create(CreateHandoutCommand command) {
        requireWorld(command.worldId());
        requireSession(command.worldId(), command.sessionId());
        Handout created = Handout.create(ids.newId(), command.worldId(), command.title(),
                toPreset(command.preset()), command.body(), command.sessionId(),
                command.revealed(), clock.instant());
        return viewMapper.toView(handouts.save(created));
    }

    @Override
    @Transactional
    public HandoutView update(UpdateHandoutCommand command) {
        requireSession(command.worldId(), command.sessionId());
        Handout handout = require(command.worldId(), command.handoutId());
        handout.update(command.title(), toPreset(command.preset()), command.body(),
                command.sessionId(), command.revealed(), clock.instant());
        return viewMapper.toView(handouts.save(handout));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID handoutId) {
        handouts.delete(require(worldId, handoutId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HandoutView> list(UUID worldId) {
        requireWorld(worldId);
        return handouts.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HandoutView get(UUID worldId, UUID handoutId) {
        return viewMapper.toView(require(worldId, handoutId));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID handoutId, UUID worldId) {
        return handouts.findByIdAndWorld(handoutId, worldId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HandoutView> findByIdInWorld(UUID handoutId, UUID worldId) {
        return handouts.findByIdAndWorld(handoutId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HandoutView> findById(UUID handoutId) {
        return handouts.findById(handoutId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HandoutView> findByWorld(UUID worldId) {
        return handouts.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HandoutView> findBySession(UUID sessionId) {
        return handouts.findBySession(sessionId).stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public HandoutView importHandout(HandoutView view) {
        Handout handout = Handout.reconstitute(view.id(), view.worldId(), view.title(),
                toPreset(view.preset()), view.body(), view.sessionId(), view.sortOrder(),
                view.revealed(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(handouts.save(handout));
    }

    @Override
    @Transactional
    public List<HandoutView> reorder(ReorderHandoutsCommand command) {
        requireWorld(command.worldId());
        List<Handout> existing = handouts.findByWorld(command.worldId());
        Set<UUID> existingIds = existing.stream().map(Handout::getId).collect(Collectors.toSet());
        if (command.orderedIds().size() != existingIds.size()
                || !existingIds.equals(new HashSet<>(command.orderedIds()))) {
            throw new ValidationException(
                    "orderedIds must be exactly this world's current handouts, in the new order");
        }
        Map<UUID, Handout> byId = existing.stream()
                .collect(Collectors.toMap(Handout::getId, Function.identity()));
        List<Handout> reordered = command.orderedIds().stream().map(byId::get).toList();
        for (int i = 0; i < reordered.size(); i++) {
            reordered.get(i).reorder(i, clock.instant());
        }
        return reordered.stream().map(h -> viewMapper.toView(handouts.save(h))).toList();
    }

    private static Preset toPreset(String raw) {
        try {
            return Preset.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown handout style: " + raw);
        }
    }

    private Handout require(UUID worldId, UUID handoutId) {
        return handouts.findByIdAndWorld(handoutId, worldId)
                .orElseThrow(() -> new NotFoundException("Handout not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireSession(UUID worldId, UUID sessionId) {
        if (sessionId != null && !sessions.existsInWorld(sessionId, worldId)) {
            throw new NotFoundException("Session not found");
        }
    }
}
