import { useId, useState, type FormEvent } from "react";
import {
  fieldErrors,
  useSetBalance,
  type Account,
} from "@/features/accounts/api";
import { inputClass, primaryButton } from "@/features/accounts/form";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";
import { parseMoneyInput } from "@/lib/money";

/** "8950.00" → "8,950", "8950.50" → "8,950.50": a starting value parseMoneyInput accepts back. */
function editableAmount(balance: string | undefined): string {
  if (!balance) return "";
  const value = Number(balance);
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: Number.isInteger(value) ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/** The inline "Balance today [amount] Update" form on a hand-entered account. */
export function ManualBalanceForm({ account }: { account: Account }) {
  const id = useId();
  const [value, setValue] = useState(() => editableAmount(account.balance));
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const setBalance = useSetBalance();
  const name = account.name ?? "";

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setSaved(false);
    const balance = parseMoneyInput(value);
    if (balance === null || !account.id) {
      setError(text.form.invalidAmount);
      return;
    }
    setError(null);
    setBalance.mutate(
      { id: account.id, balance },
      {
        onSuccess: () => {
          setValue(editableAmount(balance));
          setSaved(true);
        },
        onError: (e) => setError(fieldErrors(e).balance ?? messageFor(e)),
      },
    );
  };

  return (
    <form
      noValidate
      onSubmit={onSubmit}
      className="flex flex-col gap-1 min-[900px]:items-end"
    >
      <div className="flex w-full items-center gap-3 min-[900px]:w-auto">
        <label
          htmlFor={id}
          className="text-[13px] text-ink/70 max-[899px]:sr-only"
        >
          {text.balanceForm.label}
          <span className="sr-only"> {name}</span>
        </label>
        <input
          id={id}
          className={`${inputClass} min-w-0 flex-1 font-semibold min-[900px]:w-[120px] min-[900px]:flex-none`}
          value={value}
          onChange={(e) => {
            setValue(e.target.value);
            setSaved(false);
          }}
          inputMode="decimal"
          autoComplete="off"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        <button
          type="submit"
          className={primaryButton}
          disabled={setBalance.isPending}
        >
          {setBalance.isPending
            ? text.balanceForm.updating
            : text.balanceForm.update}
        </button>
      </div>
      {error && (
        <p id={`${id}-error`} className="text-sm text-debt">
          {error}
        </p>
      )}
      <p aria-live="polite" className="text-sm text-gain">
        {saved ? text.balanceForm.updated : ""}
      </p>
    </form>
  );
}
