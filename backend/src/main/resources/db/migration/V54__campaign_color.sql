-- Campaign color (ADR-0107): a user-assignable color badge, same shape as
-- game_systems.color (V43) — used to tell campaigns apart on the new
-- world/campaign session calendars.

ALTER TABLE campaigns ADD COLUMN color VARCHAR(20);
