-- The time zone is set from the browser at first sign-in, then changed only in Settings
-- (design.md). Null means "not set yet", so the app can tell it apart from a zone the user chose
-- and never overwrite that one. Existing rows keep their zone.
alter table users alter column time_zone drop default;
alter table users alter column time_zone drop not null;
