import { useId, type ReactNode } from "react";

type FieldProps = {
  label: string;
  error?: string;
  hint?: string;
  /** Renders the control, wired to the label, hint, and error. */
  children: (props: {
    id: string;
    "aria-invalid": boolean;
    "aria-describedby": string | undefined;
  }) => ReactNode;
};

/** A labelled form control with an optional hint and an error shown next to it. */
export function Field({ label, error, hint, children }: FieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy =
    [hint && hintId, error && errorId].filter(Boolean).join(" ") || undefined;
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      {children({
        id,
        "aria-invalid": Boolean(error),
        "aria-describedby": describedBy,
      })}
      {hint && (
        <p id={hintId} className="text-xs text-ink/70">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-sm text-debt">
          {error}
        </p>
      )}
    </div>
  );
}

/** A request-level error (not tied to one field), announced to screen readers. */
export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="text-sm text-debt">
      {message}
    </p>
  );
}
