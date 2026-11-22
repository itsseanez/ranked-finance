-- v1 allows exactly one user per install: the owner.
-- A unique index on a constant expression lets the users table hold at most one row,
-- so two registrations racing each other still can't create a second owner.
-- Multi-user support later drops this index in a new migration.
create unique index users_single_owner on users ((true));
