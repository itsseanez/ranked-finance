import { useQueryClient } from "@tanstack/react-query";
import { type FormEvent, useState } from "react";
import { Navigate, useNavigate } from "react-router";
import {
  assertPasskeysSupported,
  loginWithPasskey,
  PasskeyCancelledError,
  PasskeyUnsupportedError,
  registerPasskey,
} from "@/features/auth/passkey";
import { meQueryKey, useMe } from "@/features/auth/use-me";
import { ApiError, apiFetch, UnauthorizedError } from "@/lib/api-client";
import { messageFor } from "@/lib/errors";

const UNSUPPORTED =
  "This browser doesn't support passkeys. Try the latest Chrome, Safari, or Edge.";

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
  if (error instanceof ApiError && error.code) return messageFor(error);
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
  const me = useMe();

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
    void setUpThenSignIn();
  }

  async function setUpThenSignIn() {
    setError(null);
    setBusy(true);
    try {
      assertPasskeysSupported();
      await apiFetch("/api/v1/setup", {
        method: "POST",
        body: JSON.stringify({ code }),
      });
      await registerPasskey();
    } catch (caught) {
      setError(setupMessage(caught));
      setBusy(false);
      return;
    }
    // The setup code only unlocks passkey registration (ADR 0013). Sign in with the new passkey;
    // if that fails, the passkey still exists, so the sign-in screen is the way back in.
    setMode("sign-in");
    try {
      await loginWithPasskey();
      await finish();
    } catch (caught) {
      setError(`Your passkey is set up. ${signInMessage(caught)}`);
    } finally {
      setBusy(false);
    }
  }

  function switchMode(next: "sign-in" | "setup") {
    setError(null);
    setMode(next);
  }

  if (me.isSuccess) return <Navigate to="/" replace />;

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
              Enter the setup code, then create a passkey on this device.
            </p>
            <p className="mt-3 text-sm text-neutral-600">
              The server prints the code in its log when it starts, between two
              rows of asterisks. Running it in Docker? Run{" "}
              <code className="font-mono">docker logs</code> with the
              container's name. If you set{" "}
              <code className="font-mono">SETUP_CODE</code> yourself, use that.
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
