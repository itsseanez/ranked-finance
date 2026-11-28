-- Runs once, as POSTGRES_USER (rankedfinance, the schema owner), when the Postgres volume is empty.
-- The app connects as this role and can only read and write rows; V4 grants it that.
-- This password is for local development only and never ships (ADR 0015).
create role rankedfinance_app login password 'rankedfinance_app';
