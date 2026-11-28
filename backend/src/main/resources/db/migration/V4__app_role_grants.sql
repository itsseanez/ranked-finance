-- The app's database role reads and writes rows only; migrations run as the schema owner (ADR 0015).
-- The app_role placeholder below is filled in by Flyway from spring.flyway.placeholders.app_role,
-- which defaults to DB_USER, so the grants follow whatever role the app connects as.
-- The operator creates the role; no password ever sits in a migration.
grant usage on schema public to ${app_role};
grant select, insert, update, delete on all tables in schema public to ${app_role};
grant usage, select on all sequences in schema public to ${app_role};
alter default privileges in schema public grant select, insert, update, delete on tables to ${app_role};
alter default privileges in schema public grant usage, select on sequences to ${app_role};
revoke all on flyway_schema_history from ${app_role};
