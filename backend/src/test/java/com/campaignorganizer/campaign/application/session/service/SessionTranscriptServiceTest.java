package com.campaignorganizer.campaign.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionTranscriptRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort.LineInput;
import com.campaignorganizer.campaign.domain.session.Session;
import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import com.campaignorganizer.campaign.domain.session.TranscriptLine;
import com.campaignorganizer.campaign.domain.session.TranscriptLineKind;
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

/** Unit coverage for the transcript use cases and the companion write port (ADR-0121). */
@ExtendWith(MockitoExtension.class)
class SessionTranscriptServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private SessionTranscriptRepositoryPort transcripts;
    @Mock
    private SessionRepositoryPort sessions;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private IdGenerator ids;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private SessionTranscriptService service;

    @BeforeEach
    void setUp() {
        service = new SessionTranscriptService(transcripts, sessions, campaigns, new SessionTranscriptViewMapperImpl(),
                ids, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenSession() {
        when(sessions.findByIdAndCampaign(sessionId, campaignId)).thenReturn(Optional.of(
                Session.create(sessionId, campaignId, "S", 1, null, null, null, NOW)));
    }

    private static List<LineInput> inputs() {
        return List.of(new LineInput(500, null, "NARRATION", "Der Wirt blickt auf."),
                new LineInput(2500, "Eckart", "IN_CHARACTER", "Willkommen!"));
    }

    @Test
    void replaceCreatesTheTranscriptWithItsLinesInOrder() {
        givenSession();
        when(transcripts.findBySession(sessionId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(transcripts.save(any())).thenAnswer(i -> i.getArgument(0));

        SessionTranscriptView view = service.replace(campaignId, sessionId, inputs());

        assertThat(view.lines()).extracting(SessionTranscriptView.LineView::kind)
                .containsExactly("NARRATION", "IN_CHARACTER");
        assertThat(view.lines().get(1).speaker()).isEqualTo("Eckart");
        assertThat(view.createdAt()).isEqualTo(NOW);
    }

    @Test
    void replaceReplacesAnExistingTranscriptInsteadOfAppending() {
        givenSession();
        SessionTranscript existing = SessionTranscript.create(UUID.randomUUID(), sessionId,
                List.of(new TranscriptLine(0, null, TranscriptLineKind.NARRATION, "old")), NOW.minusSeconds(60));
        when(transcripts.findBySession(sessionId)).thenReturn(Optional.of(existing));
        when(transcripts.save(any())).thenAnswer(i -> i.getArgument(0));

        SessionTranscriptView view = service.replace(campaignId, sessionId, inputs());

        assertThat(view.lines()).hasSize(2);
        assertThat(view.createdAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(view.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void replaceRejectsAnyKindOtherThanStoryAndStoresNothing() {
        givenSession();
        for (String kind : new String[] {"META", "OFF_TOPIC", "UNCLEAR", "whatever"}) {
            List<LineInput> lines = List.of(new LineInput(0, null, kind, "Wie läuft der Job?"));
            assertThatThrownBy(() -> service.replace(campaignId, sessionId, lines))
                    .isInstanceOf(ValidationException.class);
        }
        verify(transcripts, never()).save(any());
    }

    @Test
    void replaceRejectsAnEmptyTranscript() {
        givenSession();
        when(transcripts.findBySession(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(campaignId, sessionId, List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void writesAreNotFoundForASessionOutsideTheCampaign() {
        when(sessions.findByIdAndCampaign(sessionId, campaignId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(campaignId, sessionId, inputs()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(campaignId, sessionId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getReturnsAnEmptyViewWhenThereIsNoTranscript() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        givenSession();
        when(transcripts.findBySession(sessionId)).thenReturn(Optional.empty());

        SessionTranscriptView view = service.get(worldId, campaignId, sessionId);

        assertThat(view.id()).isNull();
        assertThat(view.lines()).isEmpty();
    }

    @Test
    void getAndDeleteAreNotFoundForACampaignOutsideTheWorld() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.get(worldId, campaignId, sessionId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(worldId, campaignId, sessionId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesAnExistingTranscript() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        givenSession();
        SessionTranscript existing = SessionTranscript.create(UUID.randomUUID(), sessionId,
                List.of(new TranscriptLine(0, null, TranscriptLineKind.NARRATION, "x")), NOW);
        when(transcripts.findBySession(sessionId)).thenReturn(Optional.of(existing));

        service.delete(worldId, campaignId, sessionId);

        verify(transcripts).delete(existing);
    }
}
