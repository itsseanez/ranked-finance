import { Eye, EyeOff, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { AccountMenu, type MenuAction } from "@/features/accounts/AccountMenu";
import {
  useReopenAccount,
  useUpdateAccount,
  type Account,
  type AccountGroup,
} from "@/features/accounts/api";
import { CloseAccountDialog } from "@/features/accounts/CloseAccountDialog";
import { DeleteAccountDialog } from "@/features/accounts/DeleteAccountDialog";
import { EditAccountDialog } from "@/features/accounts/EditAccountDialog";
import { FormError } from "@/features/accounts/Field";
import { ManualBalanceForm } from "@/features/accounts/ManualBalanceForm";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";
import { formatMoney } from "@/lib/money";

/** "2026-11-28" as a local date (new Date("2026-11-28") would be UTC midnight). */
function localDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Date(year, month - 1, day);
}

/** "today", "yesterday", "3 days ago", "2 months ago", in the user's locale. */
function relativeDay(isoDate: string, today: Date = new Date()): string {
  const start = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate(),
  );
  // The server dates snapshots in the user's time zone; never show "tomorrow" if this browser lags.
  const days = Math.max(
    0,
    Math.round((start.getTime() - localDate(isoDate).getTime()) / 86_400_000),
  );
  const format = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  if (days < 30) return format.format(-days, "day");
  if (days < 365) return format.format(-Math.floor(days / 30), "month");
  return format.format(-Math.floor(days / 365), "year");
}

const formatDate = (isoDate: string) =>
  new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(
    localDate(isoDate),
  );

/** "Entered by hand · updated 3 days ago" for a manual account; "updated …" otherwise. */
function updatedLine(account: Account): string {
  const parts: string[] = [];
  if (account.source === "manual") parts.push(text.card.enteredByHand);
  if (account.balanceAsOf) {
    parts.push(text.card.updated(relativeDay(account.balanceAsOf)));
  }
  return parts.join(text.card.separator);
}

const isDimmed = (account: Account) =>
  account.hidden === true || account.closedOn != null;

function menuActions(account: Account): MenuAction[] {
  const actions: MenuAction[] =
    account.closedOn != null
      ? ["reopen"] // A closed account can't be changed until it's reopened.
      : [
          "rename",
          "editDetails",
          account.hidden ? "unhide" : "hide",
          "markClosed",
        ];
  if (account.canDelete) actions.push("delete");
  return actions;
}

const badge =
  "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium";

type Dialog = "rename" | "editDetails" | "markClosed" | "delete" | null;

function AccountRow({
  account,
  label,
  showUpdatedLine,
}: {
  account: Account;
  label: string;
  showUpdatedLine: boolean;
}) {
  const [dialog, setDialog] = useState<Dialog>(null);
  const update = useUpdateAccount();
  const reopen = useReopenAccount();
  const name = account.name ?? "";
  const closed = account.closedOn != null;
  const hidden = account.hidden === true;
  const editable = account.source === "manual" && !closed;
  const error = update.error ?? reopen.error;

  const setHidden = (value: boolean) =>
    account.id && update.mutate({ id: account.id, changes: { hidden: value } });

  const onSelect = (action: MenuAction) => {
    if (!account.id) return;
    if (action === "hide" || action === "unhide") setHidden(action === "hide");
    else if (action === "reopen") reopen.mutate(account.id);
    else setDialog(action);
  };

  const balance = account.balance
    ? formatMoney(account.balance, account.currency ?? "USD", {
        debt: account.kind === "debt",
      })
    : "";

  return (
    <li
      className={`flex flex-wrap items-center gap-x-4 gap-y-3 py-3 ${
        isDimmed(account) ? "-mx-3 rounded-xl bg-page px-3 text-ink/70" : ""
      }`}
    >
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-[15px]">{label}</span>
          {hidden && (
            <span className={`${badge} border-line bg-card text-ink/70`}>
              <EyeOff className="size-3" aria-hidden />
              {text.card.hidden}
            </span>
          )}
          {closed && account.closedOn && (
            <span className={`${badge} border-line bg-card text-ink/70`}>
              {text.card.closed(formatDate(account.closedOn))}
            </span>
          )}
          {account.needsUpdate && !closed && !hidden && (
            <span className={`${badge} border-amber-200 bg-amber-50 text-warn`}>
              <TriangleAlert className="size-3" aria-hidden />
              {text.card.needsUpdate}
            </span>
          )}
        </div>
        {showUpdatedLine && (
          <p className="text-[13px] text-ink/70">{updatedLine(account)}</p>
        )}
      </div>

      {/* Desktop: the inline form replaces the amount. Phones: the form gets its own line. */}
      <span
        className={`text-[15px] font-semibold tabular-nums ${
          editable ? "min-[900px]:hidden" : ""
        }`}
      >
        {balance}
      </span>
      {editable && (
        <div className="order-last basis-full min-[900px]:order-none min-[900px]:basis-auto">
          <ManualBalanceForm account={account} />
        </div>
      )}
      <div className="flex items-center gap-2">
        {hidden && !closed && (
          <button
            type="button"
            aria-label={text.card.unhideAccount(name)}
            onClick={() => setHidden(false)}
            className="grid size-11 place-items-center rounded-xl border border-line bg-card text-ink/70 hover:bg-black/5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
          >
            <Eye className="size-4" aria-hidden />
          </button>
        )}
        <AccountMenu
          accountName={name}
          actions={menuActions(account)}
          onSelect={onSelect}
        />
      </div>
      {error && (
        <div className="basis-full">
          <FormError message={messageFor(error)} />
        </div>
      )}

      {(dialog === "rename" || dialog === "editDetails") && (
        <EditAccountDialog
          account={account}
          mode={dialog === "rename" ? "rename" : "details"}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === "markClosed" && (
        <CloseAccountDialog account={account} onClose={() => setDialog(null)} />
      )}
      {dialog === "delete" && (
        <DeleteAccountDialog
          account={account}
          onClose={() => setDialog(null)}
        />
      )}
    </li>
  );
}

/** One institution's card: its initial, its name, and a row per account. */
export function AccountCard({ group }: { group: AccountGroup }) {
  const single = group.accounts.length === 1;
  const first = group.accounts[0];
  const subtitle = single
    ? updatedLine(first)
    : text.card.accountCount(group.accounts.length);
  const rowLabel = (account: Account) =>
    group.titledByAccount
      ? (text.kinds[account.kind ?? ""] ?? account.kind ?? "")
      : (account.name ?? "");

  return (
    <section
      aria-label={group.title}
      className={`rounded-2xl border border-line px-5 pt-5 pb-2 ${
        group.accounts.every(isDimmed) ? "bg-page" : "bg-card"
      }`}
    >
      <div className="flex items-center gap-3">
        <span
          className="grid size-9 shrink-0 place-items-center rounded-lg bg-black/5 text-sm font-semibold"
          aria-hidden
        >
          {group.title.charAt(0).toUpperCase()}
        </span>
        <div className="min-w-0">
          <h2 className="truncate text-base font-semibold">{group.title}</h2>
          {subtitle && <p className="text-[13px] text-ink/70">{subtitle}</p>}
        </div>
      </div>
      <ul className="mt-2">
        {group.accounts.map((account) => (
          <AccountRow
            key={account.id}
            account={account}
            label={rowLabel(account)}
            showUpdatedLine={!single}
          />
        ))}
      </ul>
    </section>
  );
}
