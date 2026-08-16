package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.SessionDtos.SessionResponse;
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

    public record SessionPacketResponse(
            SessionResponse session,
            String campaignName,
            List<PacketBeat> beats,
            List<PacketArticle> articles,
            List<PacketMap> maps,
            List<StatblockView> statblocks) {}
}
