import { useState } from "react";
import { useDeleteAccount, type Account } from "@/features/accounts/api";
import { FormError } from "@/features/accounts/Field";
import { dangerButton, secondaryButton } from "@/features/accounts/form";
import { Modal } from "@/features/accounts/Modal";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";

/** Confirms deleting an account. The menu offers this only when the account has no history. */
export function DeleteAccountDialog({
  account,
  onClose,
}: {
  account: Account;
  onClose: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const remove = useDeleteAccount();

  const onConfirm = () => {
    if (!account.id) return;
    remove.mutate(account.id, {
      onSuccess: onClose,
      onError: (e) => setError(messageFor(e)),
    });
  };

  return (
    <Modal title={text.remove.title(account.name ?? "")} onClose={onClose}>
      <p className="text-sm text-ink/70">{text.remove.body}</p>
      <FormError message={error} />
      <div className="mt-6 flex justify-end gap-3">
        <button type="button" className={secondaryButton} onClick={onClose}>
          {text.form.cancel}
        </button>
        <button
          type="button"
          className={dangerButton}
          disabled={remove.isPending}
          onClick={onConfirm}
        >
          {text.remove.confirm}
        </button>
      </div>
    </Modal>
  );
}
