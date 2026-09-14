package com.campaignorganizer.characters.application.statblock.service;

import com.campaignorganizer.characters.application.statblock.port.out.GlobalStatblockRepositoryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.security.CurrentUserPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves {@link GlobalStatblockQueryPort} as its own bean, separate from
 * {@link GlobalStatblockService} (ADR-0109), to break a Spring bean-construction cycle:
 * {@code GlobalStatblockService}'s {@code @PreAuthorize} methods need AOP method-security
 * infrastructure, which needs {@code WorldPermissionEvaluator}, which needs this published
 * port — if {@code GlobalStatblockService} implemented both, that's a cycle. Same fix already
 * used for {@code GlobalFieldTemplateQueryService} (ADR-0093) and {@code TagQueryService}
 * (ADR-0083).
 */
@Service
public class GlobalStatblockQueryService implements GlobalStatblockQueryPort {

    private final GlobalStatblockRepositoryPort statblocks;
    private final GlobalStatblockViewMapper viewMapper;
    private final CurrentUserPort currentUser;

    public GlobalStatblockQueryService(GlobalStatblockRepositoryPort statblocks,
                                       GlobalStatblockViewMapper viewMapper, CurrentUserPort currentUser) {
        this.statblocks = statblocks;
        this.viewMapper = viewMapper;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlobalStatblockView> findAll() {
        return statblocks.findAllByOwnerId(currentUser.currentAccountId()).stream()
                .map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GlobalStatblockView> findById(UUID globalStatblockId) {
        return statblocks.findById(globalStatblockId).map(viewMapper::toView);
    }
}
