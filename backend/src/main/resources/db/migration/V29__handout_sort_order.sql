-- Manual handout reordering: nullable so existing rows keep their current
-- created_at-DESC order until a GM explicitly reorders them (nulls sort
-- last, so newly-reordered handouts take priority over untouched ones).

ALTER TABLE handouts ADD COLUMN sort_order INT;
