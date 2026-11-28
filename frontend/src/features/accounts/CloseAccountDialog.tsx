import { useState, type FormEvent } from "react";
import {
  fieldErrors,
  useCloseAccount,
  type Account,
} from "@/features/accounts/api";
import { Field, FormError } from "@/features/accounts/Field";
import {
  inputClass,
  primaryButton,
  secondaryButton,
} from "@/features/accounts/form";
import { Modal } from "@/features/accounts/Modal";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";

/** Today in the browser's time zone as yyyy-mm-dd (not toISOString, which is UTC). */
function localToday(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Marks an account closed on a date (today by default). It counts as zero from that day. */
export function CloseAccountDialog({
  account,
  onClose,
}: {
  account: Account;
  onClose: () => void;
}) {
  const today = localToday();
  const [closedOn, setClosedOn] = useState(today);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [requestError, setRequestError] = useState<string | null>(null);
  const close = useCloseAccount();

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!account.id) return;
    close.mutate(
      { id: account.id, closedOn },
      {
        onSuccess: onClose,
        onError: (error) => {
          const byField = fieldErrors(error);
          setErrors(byField);
          setRequestError(byField.closedOn ? null : messageFor(error));
        },
      },
    );
  };

  return (
    <Modal title={text.close.title(account.name ?? "")} onClose={onClose}>
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
        <p className="text-sm text-ink/70">{text.close.body}</p>
        <Field label={text.close.date} error={errors.closedOn}>
          {(props) => (
            <input
              {...props}
              type="date"
              required
              max={today}
              className={inputClass}
              value={closedOn}
              onChange={(e) => setClosedOn(e.target.value)}
            />
          )}
        </Field>
        <FormError message={requestError} />
        <div className="mt-2 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={onClose}>
            {text.form.cancel}
          </button>
          <button
            type="submit"
            className={primaryButton}
            disabled={close.isPending || !closedOn}
          >
            {text.close.confirm}
          </button>
        </div>
      </form>
    </Modal>
  );
}
