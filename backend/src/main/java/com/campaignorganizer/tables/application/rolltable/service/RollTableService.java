package com.campaignorganizer.tables.application.rolltable.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.rolltable.port.in.CreateRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.DeleteRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.GetRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.ListRollTablesUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.CreateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.EntryInput;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.UpdateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.in.UpdateRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.out.RollTableRepositoryPort;
import com.campaignorganizer.tables.application.rolltable.port.out.WorldExistsPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableImportPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.tables.domain.rolltable.RollTable;
import com.campaignorganizer.tables.domain.rolltable.RollTableEntry;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Roll-table use cases; also implements the published query/import ports. */
@Service
public class RollTableService implements CreateRollTableUseCase, UpdateRollTableUseCase,
        DeleteRollTableUseCase, ListRollTablesUseCase, GetRollTableUseCase,
        RollTableQueryPort, RollTableImportPort {

    private final RollTableRepositoryPort tables;
    private final WorldExistsPort worlds;
    private final RollTableViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public RollTableService(RollTableRepositoryPort tables, WorldExistsPort worlds,
                            RollTableViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.tables = tables;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RollTableView create(CreateRollTableCommand command) {
        requireWorld(command.worldId());
        RollTable created = RollTable.create(ids.newId(), command.worldId(), command.title(),
                command.description(), command.diceExpression(), toEntries(command.entries()),
                clock.instant());
        return viewMapper.toView(tables.save(created));
    }

    @Override
    @Transactional
    public RollTableView update(UpdateRollTableCommand command) {
        RollTable table = require(command.worldId(), command.tableId());
        table.update(command.title(), command.description(), command.diceExpression(),
                toEntries(command.entries()), clock.instant());
        return viewMapper.toView(tables.save(table));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID tableId) {
        tables.delete(require(worldId, tableId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RollTableView> list(UUID worldId) {
        requireWorld(worldId);
        return tables.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RollTableView get(UUID worldId, UUID tableId) {
        return viewMapper.toView(require(worldId, tableId));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID tableId, UUID worldId) {
        return tables.existsInWorld(tableId, worldId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RollTableView> findByIdInWorld(UUID tableId, UUID worldId) {
        return tables.findByIdAndWorld(tableId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RollTableView> findById(UUID tableId) {
        return tables.findById(tableId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RollTableView> findByWorld(UUID worldId) {
        return tables.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public RollTableView importRollTable(RollTableView view) {
        List<RollTableEntry> entries = view.entries() == null ? List.of()
                : view.entries().stream()
                        .map(e -> new RollTableEntry(e.id(), e.minResult(), e.maxResult(), e.body()))
                        .toList();
        RollTable table = RollTable.reconstitute(view.id(), view.worldId(), view.title(),
                view.description(), view.diceExpression(), view.minResult(), view.maxResult(),
                entries, view.createdAt(), view.updatedAt());
        return viewMapper.toView(tables.save(table));
    }

    private List<RollTableEntry> toEntries(List<EntryInput> inputs) {
        return inputs == null ? List.of()
                : inputs.stream()
                        .map(e -> new RollTableEntry(ids.newId(), e.minResult(), e.maxResult(), e.body()))
                        .toList();
    }

    private RollTable require(UUID worldId, UUID tableId) {
        return tables.findByIdAndWorld(tableId, worldId)
                .orElseThrow(() -> new NotFoundException("Roll table not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
