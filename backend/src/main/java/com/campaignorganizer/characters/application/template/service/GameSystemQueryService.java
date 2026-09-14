package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.out.GameSystemRepositoryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.security.CurrentUserPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves {@link GameSystemQueryPort} as its own bean, separate from {@link GameSystemService}
 * (ADR-0109), to break a Spring bean-construction cycle: {@code GameSystemService}'s
 * {@code @PreAuthorize} methods need AOP method-security infrastructure, which needs
 * {@code WorldPermissionEvaluator}, which needs this published port — if
 * {@code GameSystemService} implemented both, that's a cycle. Same fix already used for
 * {@link GlobalFieldTemplateQueryService} (ADR-0093) and {@code TagQueryService} (ADR-0083).
 */
@Service
public class GameSystemQueryService implements GameSystemQueryPort {

    private final GameSystemRepositoryPort systems;
    private final GameSystemViewMapper viewMapper;
    private final CurrentUserPort currentUser;

    public GameSystemQueryService(GameSystemRepositoryPort systems, GameSystemViewMapper viewMapper,
                                  CurrentUserPort currentUser) {
        this.systems = systems;
        this.viewMapper = viewMapper;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSystemView> findAll() {
        return systems.findAllByOwnerId(currentUser.currentAccountId()).stream().map(viewMapper::toView).toList();
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
}
