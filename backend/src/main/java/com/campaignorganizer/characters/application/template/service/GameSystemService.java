package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.in.CreateGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.CreateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.UpdateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.in.GetGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListGameSystemsUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.out.GameSystemRepositoryPort;
import com.campaignorganizer.characters.application.template.port.out.GlobalFieldTemplateRepositoryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemImportPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.characters.domain.template.GameSystem;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Game system use cases (ADR-0094); also implements the published query/import ports. */
@Service
public class GameSystemService implements CreateGameSystemUseCase, UpdateGameSystemUseCase,
        DeleteGameSystemUseCase, GetGameSystemUseCase, ListGameSystemsUseCase, GameSystemQueryPort,
        GameSystemImportPort {

    private final GameSystemRepositoryPort systems;
    private final GlobalFieldTemplateRepositoryPort globalTemplates;
    private final GameSystemViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public GameSystemService(GameSystemRepositoryPort systems,
                             GlobalFieldTemplateRepositoryPort globalTemplates,
                             GameSystemViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.systems = systems;
        this.globalTemplates = globalTemplates;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GameSystemView create(CreateGameSystemCommand command) {
        requireNameAvailable(command.name(), null);
        GameSystem created = GameSystem.create(ids.newId(), command.name(), clock.instant());
        return viewMapper.toView(systems.save(created));
    }

    @Override
    @Transactional
    public GameSystemView update(UpdateGameSystemCommand command) {
        GameSystem system = require(command.systemId());
        requireNameAvailable(command.name(), command.systemId());
        system.update(command.name(), clock.instant());
        return viewMapper.toView(systems.save(system));
    }

    @Override
    @Transactional
    public void delete(UUID systemId) {
        GameSystem system = require(systemId);
        if (globalTemplates.existsBySystemId(systemId)) {
            throw new ConflictException("Game system is still referenced by a global field template");
        }
        systems.delete(system);
    }

    @Override
    @Transactional(readOnly = true)
    public GameSystemView get(UUID systemId) {
        return viewMapper.toView(require(systemId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSystemView> list() {
        return findAll();
    }

    // --- published import port (ADR-0061/ADR-0094): resolve-or-reuse, not blind recreate ---

    @Override
    @Transactional
    public GameSystemView importOrReuse(GameSystemView view) {
        Optional<GameSystem> existing = systems.findByNameIgnoreCase(view.name());
        if (existing.isPresent()) {
            return viewMapper.toView(existing.get());
        }
        GameSystem created = GameSystem.reconstitute(view.id(), view.name(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(systems.save(created));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<GameSystemView> findAll() {
        return systems.findAll().stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSystemView> findById(UUID systemId) {
        return systems.findById(systemId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID systemId) {
        return systems.findById(systemId).isPresent();
    }

    private GameSystem require(UUID systemId) {
        return systems.findById(systemId)
                .orElseThrow(() -> new NotFoundException("Game system not found"));
    }

    private void requireNameAvailable(String name, UUID excludingId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Game system name must not be blank");
        }
        systems.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new ConflictException("A game system named \"" + name + "\" already exists");
            }
        });
    }
}
