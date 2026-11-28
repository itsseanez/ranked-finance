import { ApiError } from "@/lib/api-client";
import type { components } from "@/lib/api-schema";

/** Every `error` code the API sends (ADR 0014), from the generated OpenAPI types. */
export type ErrorCode = components["schemas"]["ApiProblem"]["error"];

const GENERIC = "Something went wrong. Try again.";

/** One message per code. Adding a code to the backend fails the type check until it has one here. */
export const errorMessages: Record<ErrorCode, string> = {
  unauthenticated: "You're signed out. Sign in again.",
  setup_incomplete: "Finish setting up your passkey first.",
  forbidden: "That request was blocked. Reload the page and try again.",
  setup_closed: "This app is already set up. Sign in with your passkey.",
  invalid_setup_code:
    "That setup code isn't right. If the server restarted, use the newest code in its log.",
  not_found: "That couldn't be found. It may have been deleted.",
  validation_failed: "Some fields need fixing. Check them and try again.",
  conflict: "That change couldn't be made. Reload the page and try again.",
  method_not_allowed: "That action isn't supported.",
  internal_error: "Something went wrong on our end. Try again.",
  last_passkey: "You can't delete your only passkey. Add another one first.",
  owner_mismatch:
    "This server is set up for a different owner email. Check OWNER_EMAIL.",
};

function isErrorCode(code: string): code is ErrorCode {
  return Object.hasOwn(errorMessages, code);
}

/** The message to show for a failed request; codes this build doesn't know get a generic one. */
export function messageFor(error: unknown): string {
  if (error instanceof ApiError && error.code && isErrorCode(error.code)) {
    return errorMessages[error.code];
  }
  return GENERIC;
}
