-- Test only: TestDatabase.deleteAllRows empties tables with TRUNCATE, which the app role isn't
-- granted in production. Runs as the owner before Flyway, so it covers every table migrations create.
alter default privileges in schema public grant truncate on tables to rankedfinance_app;
