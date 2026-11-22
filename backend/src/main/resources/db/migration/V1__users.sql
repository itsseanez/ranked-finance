create table users (
  id                uuid primary key,
  email             text not null unique,
  display_name      text not null,
  birth_year        int,
  sex               text check (sex in ('male','female')),
  marital_status    text check (marital_status in ('never_married','married','divorced','widowed','separated')),
  living_situation  text check (living_situation in ('alone','partner','parents','other')),
  country           text,
  time_zone         text not null default 'America/New_York',
  home_currency     text not null default 'USD' check (home_currency in ('USD','CAD')),
  created_at        timestamptz not null default now()
);
