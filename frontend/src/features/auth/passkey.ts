import { apiFetch } from "@/lib/api-client";

/** The user closed the browser's passkey prompt, or it timed out. */
export class PasskeyCancelledError extends Error {
  constructor() {
    super("The passkey prompt was cancelled.");
    this.name = "PasskeyCancelledError";
  }
}

/** The browser has no WebAuthn support. */
export class PasskeyUnsupportedError extends Error {
  constructor() {
    super("This browser doesn't support passkeys.");
    this.name = "PasskeyUnsupportedError";
  }
}

/** WebAuthn plus the JSON helpers this app uses, which arrived in browsers later than WebAuthn. */
export function assertPasskeysSupported(): void {
  const credential = window.PublicKeyCredential;
  if (
    typeof credential === "undefined" ||
    typeof credential.parseCreationOptionsFromJSON !== "function" ||
    typeof credential.parseRequestOptionsFromJSON !== "function"
  ) {
    throw new PasskeyUnsupportedError();
  }
}

// Browsers raise NotAllowedError for both "cancelled" and "timed out", on purpose, so a page
// can't tell which passkeys exist.
async function promptBrowser(
  prompt: () => Promise<Credential | null>,
): Promise<PublicKeyCredential> {
  let credential: Credential | null;
  try {
    credential = await prompt();
  } catch (error) {
    if (error instanceof DOMException && error.name === "NotAllowedError") {
      throw new PasskeyCancelledError();
    }
    throw error;
  }
  if (!credential) throw new PasskeyCancelledError();
  return credential as PublicKeyCredential;
}

/** Registers a passkey for the signed-in user (body shapes from Spring Security's passkeys reference). */
export async function registerPasskey(label = "Passkey"): Promise<void> {
  assertPasskeysSupported();
  const options = await apiFetch<PublicKeyCredentialCreationOptionsJSON>(
    "/webauthn/register/options",
    { method: "POST" },
  );
  const credential = await promptBrowser(() =>
    navigator.credentials.create({
      publicKey: PublicKeyCredential.parseCreationOptionsFromJSON(options),
    }),
  );
  await apiFetch("/webauthn/register", {
    method: "POST",
    body: JSON.stringify({
      publicKey: { credential: credential.toJSON(), label },
    }),
  });
}

/** Signs in with a passkey. A refused passkey comes back from the server as UnauthorizedError. */
export async function loginWithPasskey(): Promise<void> {
  assertPasskeysSupported();
  const options = await apiFetch<PublicKeyCredentialRequestOptionsJSON>(
    "/webauthn/authenticate/options",
    { method: "POST" },
  );
  const credential = await promptBrowser(() =>
    navigator.credentials.get({
      publicKey: PublicKeyCredential.parseRequestOptionsFromJSON(options),
    }),
  );
  await apiFetch("/login/webauthn", {
    method: "POST",
    body: JSON.stringify(credential.toJSON()),
  });
}
