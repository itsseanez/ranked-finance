/** Form options and shared looks for the accounts feature. */

/** Account kinds, in the order the Type menu lists them (values the API accepts). */
export const KINDS = [
  "cash",
  "investment",
  "retirement",
  "hsa",
  "crypto",
  "debt",
];

/** Update reminders, shortest first (values the API accepts). */
export const REMINDERS = ["two_weeks", "monthly", "quarterly", "never"];

/** "19.99%" → "19.99"; the server checks the range and the decimals. */
export const cleanApr = (value: string) =>
  value.trim().replace(/%$/, "").trim();

export const primaryButton =
  "inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-accent px-5 text-[15px] font-semibold text-white hover:bg-accent/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:opacity-60";
export const secondaryButton =
  "inline-flex h-11 items-center justify-center rounded-xl border border-line bg-card px-5 text-[15px] font-medium hover:bg-black/5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:opacity-60";
export const dangerButton =
  "inline-flex h-11 items-center justify-center rounded-xl bg-debt px-5 text-[15px] font-semibold text-white hover:bg-debt/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:opacity-60";
export const inputClass =
  "h-11 w-full rounded-xl border border-line bg-card px-3 text-[15px] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-accent aria-invalid:border-debt";
