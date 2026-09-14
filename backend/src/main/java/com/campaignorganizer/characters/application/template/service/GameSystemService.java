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
import com.campaignorganizer.characters.application.template.port.published.GameSystemOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.characters.domain.template.GameSystem;
import com.campaignorganizer.security.CurrentUserPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Game system use cases (ADR-0094), scoped per account (ADR-0109); also implements the
 * published import/ownership ports. The published query port is served by the separate
 * {@link GameSystemQueryService} bean instead — see its Javadoc for why (a Spring
 * bean-construction cycle through {@code @PreAuthorize}'s AOP infrastructure).
 */
@Service
public class GameSystemService implements CreateGameSystemUseCase, UpdateGameSystemUseCase,
        DeleteGameSystemUseCase, GetGameSystemUseCase, ListGameSystemsUseCase,
        GameSystemImportPort, GameSystemOwnershipPort {

    private final GameSystemRepositoryPort systems;
    private final GlobalFieldTemplateRepositoryPort globalTemplates;
    private final GameSystemViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;
    private final CurrentUserPort currentUser;

    public GameSystemService(GameSystemRepositoryPort systems,
                             GlobalFieldTemplateRepositoryPort globalTemplates,
                             GameSystemViewMapper viewMapper, IdGenerator ids, Clock clock,
                             CurrentUserPort currentUser) {
        this.systems = systems;
        this.globalTemplates = globalTemplates;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public GameSystemView create(CreateGameSystemCommand command) {
        UUID ownerId = currentUser.currentAccountId();
        requireNameAvailable(ownerId, command.name(), null);
        GameSystem created = GameSystem.create(ids.newId(), command.name(), command.tagline(),
                command.color(), command.notes(), ownerId, clock.instant());
        return viewMapper.toView(systems.save(created));
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#command.systemId(), 'GameSystem', 'ACCESS')")
    public GameSystemView update(UpdateGameSystemCommand command) {
        GameSystem system = require(command.systemId());
        requireNameAvailable(system.getOwnerId(), command.name(), command.systemId());
        system.update(command.name(), command.tagline(), command.color(), command.notes(), clock.instant());
        return viewMapper.toView(systems.save(system));
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#systemId, 'GameSystem', 'ACCESS')")
    public void delete(UUID systemId) {
        GameSystem system = require(systemId);
        if (globalTemplates.existsBySystemId(systemId)) {
            throw new ConflictException("Game system is still referenced by a global field template");
        }
        systems.delete(system);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#systemId, 'GameSystem', 'ACCESS')")
    public GameSystemView get(UUID systemId) {
        return viewMapper.toView(require(systemId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSystemView> list() {
        return systems.findAllByOwnerId(currentUser.currentAccountId()).stream()
                .map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061/ADR-0094): resolve-or-reuse, not blind recreate ---

    @Override
    @Transactional
    public GameSystemView importOrReuse(GameSystemView view) {
        UUID ownerId = currentUser.currentAccountId();
        Optional<GameSystem> existing = systems.findByOwnerIdAndNameIgnoreCase(ownerId, view.name());
        if (existing.isPresent()) {
            return viewMapper.toView(existing.get());
        }
        GameSystem created = GameSystem.reconstitute(view.id(), view.name(), view.tagline(), view.color(),
                view.notes(), ownerId, view.createdAt(), view.updatedAt());
        return viewMapper.toView(systems.save(created));
    }

    // --- published ownership port (ADR-0109) ---

    @Override
    @Transactional
    public void assignUnownedTo(UUID ownerId) {
        systems.assignUnownedTo(ownerId);
    }

    private GameSystem require(UUID systemId) {
        return systems.findById(systemId)
                .orElseThrow(() -> new NotFoundException("Game system not found"));
    }

    private void requireNameAvailable(UUID ownerId, String name, UUID excludingId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Game system name must not be blank");
        }
        systems.findByOwnerIdAndNameIgnoreCase(ownerId, name).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new ConflictException("A game system named \"" + name + "\" already exists");
            }
        });
    }
}
