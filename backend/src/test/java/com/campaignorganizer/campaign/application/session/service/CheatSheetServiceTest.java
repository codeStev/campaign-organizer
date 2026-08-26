package com.campaignorganizer.campaign.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.FragmentInput;
import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.PutCheatSheetCommand;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.CheatSheetRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.DeckCardExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.StatblockExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.TableEntryExistsPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.campaign.domain.session.CheatSheet;
import com.campaignorganizer.campaign.domain.session.CheatSheetFragment;
import com.campaignorganizer.campaign.domain.session.Session;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for session cheat sheets (FR-37) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class CheatSheetServiceTest {

    @Mock
    private CheatSheetRepositoryPort sheets;
    @Mock
    private SessionRepositoryPort sessions;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private StatblockExistsPort statblocks;
    @Mock
    private TableEntryExistsPort tableEntries;
    @Mock
    private DeckCardExistsPort deckCards;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-05T00:00:00Z"), ZoneOffset.UTC);
    private final CheatSheetViewMapper viewMapper = new CheatSheetViewMapperImpl();

    private CheatSheetService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    // Referenced content; the service only checks existence, never loads it.
    private final UUID statblockId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();
    private final UUID deckId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CheatSheetService(sheets, sessions, campaigns, statblocks, tableEntries,
                deckCards, viewMapper, ids, clock);
    }

    private void sessionExists() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(sessions.findByIdAndCampaign(sessionId, campaignId))
                .thenReturn(Optional.of(Session.create(UUID.randomUUID(), campaignId, "S1", 1,
                        null, null, null, clock.instant())));
    }

    @Test
    void getReturnsEmptyViewWhenNothingIsSavedYet() {
        sessionExists();
        when(sheets.findBySession(sessionId)).thenReturn(Optional.empty());

        CheatSheetView view = service.get(worldId, campaignId, sessionId);

        assertThat(view.id()).isNull();
        assertThat(view.sessionId()).isEqualTo(sessionId);
        assertThat(view.fragments()).isEmpty();
    }

    @Test
    void getRequiresTheSessionToExist() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.get(worldId, campaignId, sessionId))
                .isInstanceOf(NotFoundException.class);
        verify(sheets, never()).findBySession(any());
    }

    @Test
    void putCreatesANewSheetWithValidatedFragments() {
        sessionExists();
        when(sheets.findBySession(sessionId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID());
        when(statblocks.existsInWorld(statblockId, worldId)).thenReturn(true);
        when(tableEntries.entryExistsInWorld(tableId, entryId, worldId)).thenReturn(true);
        when(deckCards.cardExistsInWorld(deckId, cardId, worldId)).thenReturn(true);
        when(sheets.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PutCheatSheetCommand command = new PutCheatSheetCommand(worldId, campaignId, sessionId,
                List.of(new FragmentInput("FREEFORM", "Check the door", null, null, null, null,
                        null),
                        new FragmentInput("STATBLOCK", null, statblockId, null, null, null, null),
                        new FragmentInput("TABLE_ROW", null, null, tableId, entryId, null, null),
                        new FragmentInput("DECK_CARD", null, null, null, null, deckId, cardId)));
        CheatSheetView view = service.put(command);

        assertThat(view.sessionId()).isEqualTo(sessionId);
        assertThat(view.fragments()).hasSize(4);
        assertThat(view.fragments().get(0).type()).isEqualTo("FREEFORM");
        assertThat(view.fragments().get(0).text()).isEqualTo("Check the door");
        assertThat(view.fragments().get(2).entryId()).isEqualTo(entryId);
        assertThat(view.fragments().get(3).type()).isEqualTo("DECK_CARD");
        assertThat(view.fragments().get(3).cardId()).isEqualTo(cardId);
        assertThat(view.createdAt()).isEqualTo(clock.instant());
        verify(sheets).save(any());
    }

    @Test
    void putReplacesAnExistingSheetInPlace() {
        sessionExists();
        Instant created = Instant.parse("2026-03-01T00:00:00Z");
        CheatSheet existing = CheatSheet.create(UUID.randomUUID(), sessionId,
                List.of(freeform("Old note")), created);
        when(sheets.findBySession(sessionId)).thenReturn(Optional.of(existing));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sheets.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheatSheetView view = service.put(new PutCheatSheetCommand(worldId, campaignId, sessionId,
                List.of(new FragmentInput("FREEFORM", "New plan", null, null, null, null, null))));

        assertThat(view.id()).isEqualTo(existing.getId());
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(clock.instant());
        assertThat(view.fragments()).hasSize(1);
    }

    @Test
    void putAcceptsAnEmptyFragmentListToClearTheSheet() {
        sessionExists();
        when(sheets.findBySession(sessionId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sheets.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheatSheetView view = service.put(new PutCheatSheetCommand(worldId, campaignId, sessionId,
                List.of()));

        assertThat(view.fragments()).isEmpty();
        verify(sheets).save(any());
    }

    @Test
    void putRejectsAnUnknownFragmentType() {
        sessionExists();

        assertThatThrownBy(() -> service.put(new PutCheatSheetCommand(worldId, campaignId,
                sessionId, List.of(new FragmentInput("SONG", null, null, null, null, null, null)))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("index 0");
    }

    @Test
    void putRejectsAMissingStatblockReference() {
        sessionExists();
        when(statblocks.existsInWorld(statblockId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.put(new PutCheatSheetCommand(worldId, campaignId,
                sessionId, List.of(new FragmentInput("STATBLOCK", null, statblockId, null, null,
                        null, null)))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Statblock not found")
                .hasMessageContaining("(fragment 0)");
        verify(sheets, never()).save(any());
    }

    @Test
    void putRejectsAMissingTableRowReference() {
        sessionExists();
        when(tableEntries.entryExistsInWorld(tableId, entryId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.put(new PutCheatSheetCommand(worldId, campaignId,
                sessionId, List.of(new FragmentInput("TABLE_ROW", null, null, tableId, entryId,
                        null, null)))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Roll-table row not found");
    }

    @Test
    void putRejectsAMissingDeckCardReference() {
        sessionExists();
        when(deckCards.cardExistsInWorld(deckId, cardId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.put(new PutCheatSheetCommand(worldId, campaignId,
                sessionId, List.of(new FragmentInput("DECK_CARD", null, null, null, null, deckId,
                        cardId)))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Deck card not found");
    }

    @Test
    void deleteRemovesTheSavedSheet() {
        sessionExists();
        CheatSheet saved = CheatSheet.create(UUID.randomUUID(), sessionId, List.of(), clock.instant());
        when(sheets.findBySession(sessionId)).thenReturn(Optional.of(saved));

        service.delete(worldId, campaignId, sessionId);

        verify(sheets).delete(saved);
    }

    @Test
    void deleteIsIdempotentWhenNoSheetIsSaved() {
        sessionExists();
        when(sheets.findBySession(sessionId)).thenReturn(Optional.empty());

        service.delete(worldId, campaignId, sessionId);

        verify(sheets, never()).delete(any());
    }

    /** Builds a valid text-only fragment without spelling out all eight fields. */
    private static CheatSheetFragment freeform(String text) {
        return new CheatSheetFragment(UUID.randomUUID(), CheatSheetFragment.Type.FREEFORM, text,
                null, null, null, null, null);
    }
}
