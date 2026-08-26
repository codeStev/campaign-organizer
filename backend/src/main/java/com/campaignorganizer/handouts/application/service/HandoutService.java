package com.campaignorganizer.handouts.application.service;

import com.campaignorganizer.handouts.application.port.in.CreateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.GetHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.CreateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.UpdateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.ListHandoutsUseCase;
import com.campaignorganizer.handouts.application.port.in.UpdateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.out.HandoutRepositoryPort;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handout use cases; also implements the published query/import ports. */
@Service
public class HandoutService implements CreateHandoutUseCase, UpdateHandoutUseCase,
        DeleteHandoutUseCase, ListHandoutsUseCase, GetHandoutUseCase,
        HandoutQueryPort, HandoutImportPort {

    private final HandoutRepositoryPort handouts;
    private final WorldExistsPort worlds;
    private final HandoutViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public HandoutService(HandoutRepositoryPort handouts, WorldExistsPort worlds,
                          HandoutViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.handouts = handouts;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HandoutView create(CreateHandoutCommand command) {
        requireWorld(command.worldId());
        Handout created = Handout.create(ids.newId(), command.worldId(), command.title(),
                toPreset(command.preset()), command.body(), clock.instant());
        return viewMapper.toView(handouts.save(created));
    }

    @Override
    @Transactional
    public HandoutView update(UpdateHandoutCommand command) {
        Handout handout = require(command.worldId(), command.handoutId());
        handout.update(command.title(), toPreset(command.preset()), command.body(),
                clock.instant());
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

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public HandoutView importHandout(HandoutView view) {
        Handout handout = Handout.reconstitute(view.id(), view.worldId(), view.title(),
                Preset.valueOf(view.preset()), view.body(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(handouts.save(handout));
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
}
