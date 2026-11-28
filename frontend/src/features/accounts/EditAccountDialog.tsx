import { useState, type FormEvent } from "react";
import {
  fieldErrors,
  useUpdateAccount,
  type Account,
  type UpdateAccount,
} from "@/features/accounts/api";
import { Field, FormError } from "@/features/accounts/Field";
import {
  cleanApr,
  inputClass,
  primaryButton,
  REMINDERS,
  secondaryButton,
} from "@/features/accounts/form";
import { Modal } from "@/features/accounts/Modal";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";
import { parseMoneyInput } from "@/lib/money";

type Mode = "rename" | "details";

/** "8950.00" → "8950", "8950.50" → "8950.50": a starting value for an amount input. */
const plainAmount = (value: string | undefined) =>
  value ? value.replace(/\.00$/, "") : "";

/** Rename (the name only) or Edit details (institution, reminder, and a debt's rate and payment). */
export function EditAccountDialog({
  account,
  mode,
  onClose,
}: {
  account: Account;
  mode: Mode;
  onClose: () => void;
}) {
  const isDebt = account.kind === "debt";
  const [name, setName] = useState(account.name ?? "");
  const [institution, setInstitution] = useState(account.institutionName ?? "");
  const [apr, setApr] = useState(account.apr ?? "");
  const [minimumPayment, setMinimumPayment] = useState(
    plainAmount(account.minimumPayment),
  );
  const [reminder, setReminder] = useState(account.updateReminder ?? "monthly");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [requestError, setRequestError] = useState<string | null>(null);
  const update = useUpdateAccount();

  const readForm = (): UpdateAccount | null => {
    if (mode === "rename") {
      if (!name.trim()) {
        setErrors({ name: text.form.nameRequired });
        return null;
      }
      return { name: name.trim() };
    }
    const changes: UpdateAccount = {
      // A blank institution clears it; the account then gets a card of its own.
      institutionName: institution.trim(),
      updateReminder: reminder,
    };
    if (isDebt) {
      if (!cleanApr(apr)) {
        setErrors({ apr: text.form.aprRequired });
        return null;
      }
      const payment = minimumPayment.trim()
        ? parseMoneyInput(minimumPayment)
        : "";
      if (payment === null) {
        setErrors({ minimumPayment: text.form.invalidAmount });
        return null;
      }
      changes.apr = cleanApr(apr);
      changes.minimumPayment = payment; // "" clears it
    }
    return changes;
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setErrors({});
    setRequestError(null);
    const changes = readForm();
    if (!changes || !account.id) return;
    update.mutate(
      { id: account.id, changes },
      {
        onSuccess: onClose,
        onError: (error) => {
          const byField = fieldErrors(error);
          setErrors(byField);
          setRequestError(
            Object.keys(byField).length > 0 ? null : messageFor(error),
          );
        },
      },
    );
  };

  return (
    <Modal
      title={mode === "rename" ? text.rename.title : text.edit.title}
      onClose={onClose}
    >
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
        {mode === "rename" ? (
          <Field label={text.form.name} error={errors.name}>
            {(props) => (
              <input
                {...props}
                className={inputClass}
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoComplete="off"
              />
            )}
          </Field>
        ) : (
          <>
            <Field
              label={text.form.institution}
              hint={text.form.institutionHint}
              error={errors.institutionName}
            >
              {(props) => (
                <input
                  {...props}
                  className={inputClass}
                  value={institution}
                  onChange={(e) => setInstitution(e.target.value)}
                  autoComplete="off"
                />
              )}
            </Field>
            {isDebt && (
              <>
                <Field label={text.form.apr} error={errors.apr}>
                  {(props) => (
                    <input
                      {...props}
                      className={inputClass}
                      value={apr}
                      onChange={(e) => setApr(e.target.value)}
                      inputMode="decimal"
                      autoComplete="off"
                    />
                  )}
                </Field>
                <Field
                  label={text.form.minimumPayment}
                  error={errors.minimumPayment}
                >
                  {(props) => (
                    <input
                      {...props}
                      className={inputClass}
                      value={minimumPayment}
                      onChange={(e) => setMinimumPayment(e.target.value)}
                      inputMode="decimal"
                      autoComplete="off"
                    />
                  )}
                </Field>
              </>
            )}
            <Field label={text.form.reminder} error={errors.updateReminder}>
              {(props) => (
                <select
                  {...props}
                  className={inputClass}
                  value={reminder}
                  onChange={(e) => setReminder(e.target.value)}
                >
                  {REMINDERS.map((r) => (
                    <option key={r} value={r}>
                      {text.reminders[r]}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </>
        )}
        <FormError message={requestError} />
        <div className="mt-2 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={onClose}>
            {text.form.cancel}
          </button>
          <button
            type="submit"
            className={primaryButton}
            disabled={update.isPending}
          >
            {update.isPending ? text.form.saving : text.form.save}
          </button>
        </div>
      </form>
    </Modal>
  );
}
