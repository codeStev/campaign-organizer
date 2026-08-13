-- Let a story beat link multiple articles (place, NPC, item…). See ADR-0032.
-- Replaces arc_beats.article_id with a join table; existing links are backfilled.

CREATE TABLE beat_articles (
    beat_id    UUID NOT NULL REFERENCES arc_beats(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE
);

CREATE INDEX idx_beat_articles_beat ON beat_articles (beat_id);
CREATE INDEX idx_beat_articles_article ON beat_articles (article_id);

INSERT INTO beat_articles (beat_id, article_id)
SELECT id, article_id FROM arc_beats WHERE article_id IS NOT NULL;

ALTER TABLE arc_beats DROP COLUMN article_id;
