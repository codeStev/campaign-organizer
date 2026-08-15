package com.campaignorganizer.whiteboard.adapter.out.persistence;

import com.campaignorganizer.whiteboard.application.port.out.WhiteboardRepositoryPort;
import com.campaignorganizer.whiteboard.domain.Whiteboard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the whiteboard repository port. */
@Component
public class WhiteboardPersistenceAdapter implements WhiteboardRepositoryPort {

    private final WhiteboardJpaRepository repository;
    private final WhiteboardPersistenceMapper mapper;

    public WhiteboardPersistenceAdapter(WhiteboardJpaRepository repository,
                                        WhiteboardPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Whiteboard> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Whiteboard> findByIdAndWorld(UUID whiteboardId, UUID worldId) {
        return repository.findByIdAndWorldId(whiteboardId, worldId).map(mapper::toDomain);
    }

    @Override
    public Whiteboard save(Whiteboard whiteboard) {
        return mapper.toDomain(repository.save(mapper.toEntity(whiteboard)));
    }

    @Override
    public void delete(Whiteboard whiteboard) {
        repository.deleteById(whiteboard.getId());
    }
}
