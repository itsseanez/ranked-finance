import { useQueryClient } from "@tanstack/react-query";
import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router";
import {
  assertPasskeysSupported,
  loginWithPasskey,
  PasskeyCancelledError,
  PasskeyUnsupportedError,
  registerPasskey,
} from "@/features/auth/passkey";
import { meQueryKey } from "@/features/auth/use-me";
import { ApiError, apiFetch, UnauthorizedError } from "@/lib/api-client";

const UNSUPPORTED =
  "This browser doesn't support passkeys. Try the latest Chrome, Safari, or Edge.";

const SETUP_MESSAGES: Record<string, string> = {
  invalid_setup_code: "That setup code isn't right.",
  setup_closed: "This app is already set up. Sign in with your passkey.",
  setup_not_configured:
    "Setup isn't configured on this server. Set SETUP_CODE and restart.",
};

function signInMessage(error: unknown): string {
  if (error instanceof PasskeyCancelledError)
    return "Sign-in was cancelled. Try again.";
  if (error instanceof PasskeyUnsupportedError) return UNSUPPORTED;
  if (error instanceof UnauthorizedError)
    return "That passkey isn't registered with this app.";
  return "Couldn't sign in. Try again.";
}

function setupMessage(error: unknown): string {
  if (error instanceof PasskeyCancelledError)
    return "Passkey setup was cancelled. Try again.";
  if (error instanceof PasskeyUnsupportedError) return UNSUPPORTED;
  if (error instanceof ApiError && error.code && SETUP_MESSAGES[error.code]) {
    return SETUP_MESSAGES[error.code];
  }
  return "Setup didn't finish. Try again.";
}

/** Nine bars, Iron to Legend: the climb the app is about. */
function RankLadder() {
  return (
    <div aria-hidden="true" className="flex h-10 items-end gap-1">
      {Array.from({ length: 9 }, (_, i) => (
        <span
          key={i}
          className={
            i === 8
              ? "w-2 rounded-sm bg-accent"
              : "w-2 rounded-sm bg-accent-tint"
          }
          style={{ height: `${(i + 1) * 11}%` }}
        />
      ))}
    </div>
  );
}

const button =
  "w-full rounded-lg bg-accent px-4 py-3 font-medium text-white transition-opacity hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:opacity-60";
const linkButton =
  "text-sm font-medium text-accent underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent";

export function LoginPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<"sign-in" | "setup">("sign-in");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function finish() {
    // Drop the cached 401 so the auth gate fetches the signed-in user fresh.
    queryClient.removeQueries({ queryKey: meQueryKey });
    await navigate("/", { replace: true });
  }

  async function run(
    action: () => Promise<void>,
    toMessage: (error: unknown) => string,
  ) {
    setError(null);
    setBusy(true);
    try {
      await action();
      await finish();
    } catch (caught) {
      setError(toMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  function signIn() {
    void run(loginWithPasskey, signInMessage);
  }

  function setUp(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      // Check first, so a browser that can't make a passkey never signs in without one.
      assertPasskeysSupported();
      await apiFetch("/api/v1/setup", {
        method: "POST",
        body: JSON.stringify({ code }),
      });
      await registerPasskey();
    }, setupMessage);
  }

  function switchMode(next: "sign-in" | "setup") {
    setError(null);
    setMode(next);
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <RankLadder />
        <h1 className="mt-6 text-3xl font-semibold tracking-tight">
          Ranked Finance
        </h1>

        {mode === "sign-in" ? (
          <>
            <p className="mt-2 text-neutral-600">
              Sign in to pick up where you left off.
            </p>
            <button
              type="button"
              className={`${button} mt-8`}
              onClick={signIn}
              disabled={busy}
            >
              Continue with passkey
            </button>
          </>
        ) : (
          <form onSubmit={setUp} className="mt-2">
            <p className="text-neutral-600">
              Enter the setup code from your server, then create a passkey on
              this device.
            </p>
            <label
              htmlFor="setup-code"
              className="mt-8 block text-sm font-medium"
            >
              Setup code
            </label>
            <input
              id="setup-code"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              autoComplete="off"
              spellCheck={false}
              required
              className="mt-2 w-full rounded-lg border border-line bg-card px-3 py-2.5 focus-visible:outline-2 focus-visible:outline-accent"
            />
            <button type="submit" className={`${button} mt-4`} disabled={busy}>
              Create passkey
            </button>
          </form>
        )}

        {error && (
          <p role="alert" className="mt-4 text-sm text-debt">
            {error}
          </p>
        )}

        <div className="mt-8 border-t border-line pt-6">
          {mode === "sign-in" ? (
            <button
              type="button"
              className={linkButton}
              onClick={() => switchMode("setup")}
            >
              First-time setup
            </button>
          ) : (
            <button
              type="button"
              className={linkButton}
              onClick={() => switchMode("sign-in")}
            >
              Back to sign in
            </button>
          )}
        </div>
      </div>
    </main>
  );
}
