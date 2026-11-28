import { useState, type FormEvent } from "react";
import {
  fieldErrors,
  useCreateManualAccount,
  useFindMatches,
  useSetBalance,
  type AccountMatch,
  type CreateManualAccount,
} from "@/features/accounts/api";
import { Field, FormError } from "@/features/accounts/Field";
import {
  cleanApr,
  inputClass,
  KINDS,
  primaryButton,
  REMINDERS,
  secondaryButton,
} from "@/features/accounts/form";
import { Modal } from "@/features/accounts/Modal";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";
import { parseMoneyInput } from "@/lib/money";

type Errors = Record<string, string>;

/**
 * Adds a hand-entered account. Before creating it, asks the server for look-alikes; if there is
 * one, asks "Is this the same as {name}?" so nothing is counted twice.
 */
export function AddManualAccountDialog({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState("");
  const [institution, setInstitution] = useState("");
  const [kind, setKind] = useState("cash");
  const [balance, setBalance] = useState("");
  const [apr, setApr] = useState("");
  const [minimumPayment, setMinimumPayment] = useState("");
  const [reminder, setReminder] = useState("monthly");
  const [errors, setErrors] = useState<Errors>({});
  const [requestError, setRequestError] = useState<string | null>(null);
  // The account to create and the existing one it might duplicate, while the question is up.
  const [pending, setPending] = useState<{
    request: CreateManualAccount;
    match: AccountMatch;
  } | null>(null);

  const findMatches = useFindMatches();
  const create = useCreateManualAccount();
  const setExistingBalance = useSetBalance();
  const busy =
    findMatches.isPending || create.isPending || setExistingBalance.isPending;
  const isDebt = kind === "debt";

  const showError = (error: unknown) => {
    const byField = fieldErrors(error);
    setPending(null);
    setErrors(byField);
    setRequestError(Object.keys(byField).length > 0 ? null : messageFor(error));
  };

  /** The request from the form, or null after showing what needs fixing. */
  const readForm = (): CreateManualAccount | null => {
    const found: Errors = {};
    const amount = parseMoneyInput(balance);
    const payment = minimumPayment.trim()
      ? parseMoneyInput(minimumPayment)
      : "";
    if (!name.trim()) found.name = text.form.nameRequired;
    if (amount === null) found.balance = text.form.invalidAmount;
    if (isDebt && !cleanApr(apr)) found.apr = text.form.aprRequired;
    if (isDebt && payment === null)
      found.minimumPayment = text.form.invalidAmount;
    setErrors(found);
    setRequestError(null);
    if (Object.keys(found).length > 0 || amount === null) return null;
    return {
      name: name.trim(),
      institutionName: institution.trim() || undefined,
      kind,
      balance: amount,
      apr: isDebt ? cleanApr(apr) : undefined,
      minimumPayment: isDebt && payment ? payment : undefined,
      updateReminder: reminder,
    };
  };

  const createAccount = async (request: CreateManualAccount) => {
    try {
      await create.mutateAsync(request);
      onClose();
    } catch (error) {
      showError(error);
    }
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const request = readForm();
    if (!request) return;
    try {
      const matches = await findMatches.mutateAsync(request);
      if (matches.length > 0) {
        setPending({ request, match: matches[0] });
        return;
      }
    } catch (error) {
      showError(error);
      return;
    }
    await createAccount(request);
  };

  const onSame = async () => {
    if (!pending?.match.id) return;
    try {
      await setExistingBalance.mutateAsync({
        id: pending.match.id,
        balance: pending.request.balance,
      });
      onClose();
    } catch (error) {
      showError(error);
    }
  };

  if (pending) {
    return (
      <Modal
        title={text.add.duplicateQuestion(pending.match.name ?? "")}
        onClose={onClose}
      >
        <p className="text-sm text-ink/70">{text.add.duplicateHint}</p>
        <FormError message={requestError} />
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            className={secondaryButton}
            disabled={busy}
            onClick={() => void createAccount(pending.request)}
          >
            {text.add.no}
          </button>
          <button
            type="button"
            className={primaryButton}
            disabled={busy}
            onClick={() => void onSame()}
          >
            {text.add.yes}
          </button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title={text.add.title} onClose={onClose}>
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
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
        <Field label={text.form.kind} error={errors.kind}>
          {(props) => (
            <select
              {...props}
              className={inputClass}
              value={kind}
              onChange={(e) => setKind(e.target.value)}
            >
              {KINDS.map((k) => (
                <option key={k} value={k}>
                  {text.kinds[k]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field
          label={text.form.balance}
          hint={isDebt ? text.form.balanceHint : undefined}
          error={errors.balance}
        >
          {(props) => (
            <input
              {...props}
              className={inputClass}
              value={balance}
              onChange={(e) => setBalance(e.target.value)}
              inputMode="decimal"
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
        <FormError message={requestError} />
        <div className="mt-2 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={onClose}>
            {text.form.cancel}
          </button>
          <button type="submit" className={primaryButton} disabled={busy}>
            {busy ? text.add.checking : text.add.submit}
          </button>
        </div>
      </form>
    </Modal>
  );
}
