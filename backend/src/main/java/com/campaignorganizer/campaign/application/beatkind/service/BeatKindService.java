package com.campaignorganizer.campaign.application.beatkind.service;

import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.CreateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.UpdateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.in.CreateBeatKindUseCase;
import com.campaignorganizer.campaign.application.beatkind.port.in.DeleteBeatKindUseCase;
import com.campaignorganizer.campaign.application.beatkind.port.in.GetBeatKindUseCase;
import com.campaignorganizer.campaign.application.beatkind.port.in.ListBeatKindsUseCase;
import com.campaignorganizer.campaign.application.beatkind.port.in.UpdateBeatKindUseCase;
import com.campaignorganizer.campaign.application.beatkind.port.out.BeatKindRepositoryPort;
import com.campaignorganizer.campaign.application.beatkind.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindImportPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindQueryPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Beat kind use cases (ADR-0101); also implements the published query/import ports. */
@Service
public class BeatKindService implements CreateBeatKindUseCase, UpdateBeatKindUseCase,
        DeleteBeatKindUseCase, GetBeatKindUseCase, ListBeatKindsUseCase, BeatKindQueryPort,
        BeatKindImportPort {

    private final BeatKindRepositoryPort beatKinds;
    private final WorldExistsPort worlds;
    private final BeatKindViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public BeatKindService(BeatKindRepositoryPort beatKinds, WorldExistsPort worlds,
                           BeatKindViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.beatKinds = beatKinds;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BeatKindView create(CreateBeatKindCommand command) {
        requireWorld(command.worldId());
        requireNameAvailable(command.worldId(), command.name(), null);
        BeatKind created = BeatKind.create(ids.newId(), command.worldId(), command.name(), command.color(),
                clock.instant());
        return viewMapper.toView(beatKinds.save(created));
    }

    @Override
    @Transactional
    public BeatKindView update(UpdateBeatKindCommand command) {
        BeatKind beatKind = require(command.worldId(), command.beatKindId());
        requireNameAvailable(command.worldId(), command.name(), command.beatKindId());
        beatKind.update(command.name(), command.color(), clock.instant());
        return viewMapper.toView(beatKinds.save(beatKind));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID beatKindId) {
        beatKinds.delete(require(worldId, beatKindId));
    }

    @Override
    @Transactional(readOnly = true)
    public BeatKindView get(UUID worldId, UUID beatKindId) {
        return viewMapper.toView(require(worldId, beatKindId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeatKindView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public BeatKindView importBeatKind(BeatKindView view) {
        BeatKind beatKind = BeatKind.reconstitute(view.id(), view.worldId(), view.name(), view.color(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(beatKinds.save(beatKind));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<BeatKindView> findByWorld(UUID worldId) {
        return beatKinds.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID beatKindId, UUID worldId) {
        return beatKinds.findByIdAndWorld(beatKindId, worldId).isPresent();
    }

    private BeatKind require(UUID worldId, UUID beatKindId) {
        return beatKinds.findByIdAndWorld(beatKindId, worldId)
                .orElseThrow(() -> new NotFoundException("Beat kind not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireNameAvailable(UUID worldId, String name, UUID excludingId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Beat kind name must not be blank");
        }
        beatKinds.findByNameIgnoreCaseAndWorld(name, worldId).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new ConflictException("A beat kind named \"" + name + "\" already exists in this world");
            }
        });
    }
}
