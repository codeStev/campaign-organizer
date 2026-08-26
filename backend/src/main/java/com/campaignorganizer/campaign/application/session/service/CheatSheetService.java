package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.FragmentInput;
import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.PutCheatSheetCommand;
import com.campaignorganizer.campaign.application.session.port.in.DeleteCheatSheetUseCase;
import com.campaignorganizer.campaign.application.session.port.in.GetCheatSheetUseCase;
import com.campaignorganizer.campaign.application.session.port.in.PutCheatSheetUseCase;
import com.campaignorganizer.campaign.application.session.port.out.CheatSheetRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.DeckCardExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.TableEntryExistsPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetImportPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.campaign.domain.session.CheatSheet;
import com.campaignorganizer.campaign.domain.session.CheatSheetFragment;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cheat-sheet use cases (FR-37); also implements the published query and
 * import ports. Upsert validates every referenced statblock / table row /
 * deck card against the owning context so a sheet can never point at
 * something that does not exist.
 */
@Service
public class CheatSheetService implements GetCheatSheetUseCase, PutCheatSheetUseCase,
        DeleteCheatSheetUseCase, CheatSheetQueryPort, CheatSheetImportPort {

    private final CheatSheetRepositoryPort sheets;
    private final SessionRepositoryPort sessions;
    private final CampaignExistsPort campaigns;
    private final StatblockExistsPort statblocks;
    private final TableEntryExistsPort tableEntries;
    private final DeckCardExistsPort deckCards;
    private final CheatSheetViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CheatSheetService(CheatSheetRepositoryPort sheets, SessionRepositoryPort sessions,
                             CampaignExistsPort campaigns, StatblockExistsPort statblocks,
                             TableEntryExistsPort tableEntries, DeckCardExistsPort deckCards,
                             CheatSheetViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.sheets = sheets;
        this.sessions = sessions;
        this.campaigns = campaigns;
        this.statblocks = statblocks;
        this.tableEntries = tableEntries;
        this.deckCards = deckCards;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CheatSheetView get(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        return sheets.findBySession(sessionId).map(viewMapper::toView)
                .orElseGet(() -> emptyView(sessionId));
    }

    @Override
    @Transactional
    public CheatSheetView put(PutCheatSheetCommand command) {
        requireSession(command.worldId(), command.campaignId(), command.sessionId());
        List<CheatSheetFragment> fragments = toValidatedFragments(command.worldId(),
                command.fragments());
        CheatSheet saved = sheets.findBySession(command.sessionId())
                .map(existing -> {
                    existing.update(fragments, clock.instant());
                    return existing;
                })
                .orElseGet(() -> CheatSheet.create(ids.newId(), command.sessionId(), fragments,
                        clock.instant()));
        return viewMapper.toView(sheets.save(saved));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        sheets.findBySession(sessionId).ifPresent(sheets::delete);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public Optional<CheatSheetView> findBySession(UUID sessionId) {
        return sheets.findBySession(sessionId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySession(UUID sessionId) {
        return sheets.findBySession(sessionId).isPresent();
    }

    // --- published import port (ADR-0061): references were validated on export ---

    @Override
    @Transactional
    public CheatSheetView importCheatSheet(CheatSheetView view) {
        List<CheatSheetFragment> fragments = view.fragments() == null ? List.of()
                : view.fragments().stream().map(this::toFragment).toList();
        CheatSheet sheet = CheatSheet.reconstitute(view.id(), view.sessionId(), fragments,
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(sheets.save(sheet));
    }

    private CheatSheetFragment toFragment(CheatSheetView.FragmentView f) {
        return new CheatSheetFragment(f.id(), CheatSheetFragment.Type.valueOf(f.type()), f.text(),
                f.statblockId(), f.tableId(), f.entryId(), f.deckId(), f.cardId());
    }

    /** Validates each input against its owning context, then builds the domain fragment. */
    private List<CheatSheetFragment> toValidatedFragments(UUID worldId, List<FragmentInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            // An empty sheet is legitimate — the GM cleared it.
            return List.of();
        }
        List<CheatSheetFragment> out = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            FragmentInput in = inputs.get(i);
            CheatSheetFragment.Type type = parseType(in.type(), i);
            validateRefs(type, in, worldId, i);
            out.add(new CheatSheetFragment(ids.newId(), type, in.text(), in.statblockId(),
                    in.tableId(), in.entryId(), in.deckId(), in.cardId()));
        }
        return out;
    }

    private static CheatSheetFragment.Type parseType(String raw, int index) {
        try {
            return CheatSheetFragment.Type.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown cheat-sheet fragment type at index " + index
                    + ": " + raw);
        }
    }

    private void validateRefs(CheatSheetFragment.Type type, FragmentInput in, UUID worldId,
                              int index) {
        String where = " (fragment " + index + ")";
        switch (type) {
            case STATBLOCK -> {
                if (!statblocks.existsInWorld(in.statblockId(), worldId)) {
                    throw new ValidationException("Statblock not found in world" + where);
                }
            }
            case TABLE_ROW -> {
                if (!tableEntries.entryExistsInWorld(in.tableId(), in.entryId(), worldId)) {
                    throw new ValidationException("Roll-table row not found in world" + where);
                }
            }
            case DECK_CARD -> {
                if (!deckCards.cardExistsInWorld(in.deckId(), in.cardId(), worldId)) {
                    throw new ValidationException("Deck card not found in world" + where);
                }
            }
            case FREEFORM -> {
                // text-only; the domain enforces non-blank text
            }
        }
    }

    private static CheatSheetView emptyView(UUID sessionId) {
        return new CheatSheetView(null, sessionId, List.of(), null, null);
    }

    private void requireSession(UUID worldId, UUID campaignId, UUID sessionId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
        sessions.findByIdAndCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }
}
