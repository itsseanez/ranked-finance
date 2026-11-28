create table connections (
  id                      uuid primary key,
  user_id                 uuid not null references users(id),
  provider                text not null check (provider in ('plaid','simplefin')),
  institution_name        text,
  encrypted_credential    bytea not null,       -- TokenCipher output; never logged or returned
  status                  text not null default 'active' check (status in ('active','needs_reconnect','removed')),
  last_successful_update  timestamptz,
  created_at              timestamptz not null default now()
);
create index connections_user on connections (user_id);

create table accounts (
  id                uuid primary key,
  user_id           uuid not null references users(id),
  connection_id     uuid references connections(id),
  source            text not null check (source in ('provider','import','manual')),
  name              text not null,
  institution_name  text,
  last4             text check (last4 ~ '^[0-9]{4}$'),
  kind              text not null check (kind in ('cash','investment','retirement','hsa','crypto','debt')),
  currency          text not null check (currency in ('USD','CAD')),
  apr               numeric(6,3) check (apr between 0 and 100),
  minimum_payment   numeric(14,2) check (minimum_payment >= 0),
  update_reminder   text not null default 'monthly' check (update_reminder in ('two_weeks','monthly','quarterly','never')),
  hidden            boolean not null default false,
  closed_on         date,
  created_at        timestamptz not null default now(),
  check (source <> 'provider' or connection_id is not null),
  check (kind <> 'debt' or source <> 'manual' or apr is not null)
);
create index accounts_user on accounts (user_id);
create index accounts_connection on accounts (connection_id);

create table balance_snapshots (
  account_id  uuid not null references accounts(id) on delete cascade,
  user_id     uuid not null references users(id),
  date        date not null,
  balance     numeric(14,2) not null,
  primary key (account_id, date)
);
create index balance_snapshots_user_date on balance_snapshots (user_id, date);
