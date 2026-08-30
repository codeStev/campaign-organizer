package com.campaignorganizer.interchange.packet.application.port.in;

import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import java.util.List;
import java.util.UUID;

/** DTOs for the one-page session prep packet (ADR-0036). */
public final class SessionPacketDtos {

    private SessionPacketDtos() {}

    /** A beat scheduled into the session, with its arc for context. */
    public record PacketBeat(
            UUID id,
            String title,
            String body,
            boolean done,
            String arcTitle,
            List<UUID> articleIds) {}

    /** An article referenced by the session's beats, rendered for print. */
    public record PacketArticle(
            UUID id,
            String title,
            String template,
            String bodyHtml) {}

    /** A pin on a packet map; label is resolved (own label or linked article title). */
    public record PacketPin(
            double x,
            double y,
            String label) {}

    /** A map reachable from the session's beats (via a pin to a beat article). */
    public record PacketMap(
            UUID id,
            String name,
            String imageUrl,
            List<PacketPin> pins) {}

    /** One roll-table entry with its outcome rendered for print (FR-40). */
    public record PacketRollTableEntry(
            Integer minResult,
            Integer maxResult,
            String bodyHtml) {}

    /** A roll table referenced by the session's beats, entries print-ready. */
    public record PacketRollTable(
            UUID id,
            String title,
            String diceExpression,
            int minResult,
            int maxResult,
            List<PacketRollTableEntry> entries) {}

    /** One deck card with its body rendered for print. */
    public record PacketDeckCard(
            String title,
            String bodyHtml) {}

    /** A card deck referenced by the session's beats, cards print-ready. */
    public record PacketCardDeck(
            UUID id,
            String title,
            List<PacketDeckCard> cards) {}

    /**
     * A handout tagged to this session (ADR-0077). Body is raw markdown, not
     * pre-rendered HTML: handouts render client-side like the rest of a
     * packet's freeform text (session summary, GM notes, beat bodies), and
     * each preset carries its own client-side stylesheet.
     */
    public record PacketHandout(
            UUID id,
            String title,
            String preset,
            String body) {}

    public record SessionPacketResponse(
            SessionView session,
            String campaignName,
            List<PacketBeat> beats,
            List<PacketArticle> articles,
            List<PacketMap> maps,
            List<StatblockView> statblocks,
            List<PacketRollTable> rollTables,
            List<PacketCardDeck> cardDecks,
            List<PacketHandout> handouts) {}
}
