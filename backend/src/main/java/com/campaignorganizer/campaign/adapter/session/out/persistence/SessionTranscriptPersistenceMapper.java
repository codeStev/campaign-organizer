package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import com.campaignorganizer.campaign.domain.session.TranscriptLine;
import com.campaignorganizer.campaign.domain.session.TranscriptLineKind;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface SessionTranscriptPersistenceMapper {

    SessionTranscriptJpaEntity toEntity(SessionTranscript transcript);

    default TranscriptLineJson toJson(TranscriptLine line) {
        return new TranscriptLineJson(line.offsetMs(), line.speaker(), line.kind().name(), line.text());
    }

    default TranscriptLine toLine(TranscriptLineJson json) {
        return new TranscriptLine(json.offsetMs(), json.speaker(), TranscriptLineKind.parse(json.kind()), json.text());
    }

    default SessionTranscript toDomain(SessionTranscriptJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return SessionTranscript.reconstitute(entity.getId(), entity.getSessionId(),
                entity.getLines().stream().map(this::toLine).toList(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
