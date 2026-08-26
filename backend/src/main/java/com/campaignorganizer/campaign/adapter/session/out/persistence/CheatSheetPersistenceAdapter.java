package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.application.session.port.out.CheatSheetRepositoryPort;
import com.campaignorganizer.campaign.domain.session.CheatSheet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the cheat-sheet repository port. */
@Component
public class CheatSheetPersistenceAdapter implements CheatSheetRepositoryPort {

    private final CheatSheetJpaRepository repository;
    private final CheatSheetPersistenceMapper mapper;

    public CheatSheetPersistenceAdapter(CheatSheetJpaRepository repository,
                                        CheatSheetPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CheatSheet> findBySession(UUID sessionId) {
        return repository.findBySessionId(sessionId).map(mapper::toDomain);
    }

    @Override
    public CheatSheet save(CheatSheet sheet) {
        return mapper.toDomain(repository.save(mapper.toEntity(sheet)));
    }

    @Override
    public void delete(CheatSheet sheet) {
        repository.deleteById(sheet.getId());
    }
}
