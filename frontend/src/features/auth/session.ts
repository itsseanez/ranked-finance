import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api-client";
import type { ErrorCode } from "@/lib/errors";

/**
 * Where a failed request should send the user, or null if it doesn't mean "signed out". Keyed on
 * the error code (ADR 0014), never the status: a 403 `forbidden` (for example a CSRF failure) is
 * not a sign-out.
 */
export function signedOutRedirect(
  error: unknown,
  currentPath: string,
): string | null {
  if (!(error instanceof ApiError)) return null;
  const code: ErrorCode | undefined = error.code as ErrorCode | undefined;
  if (code === "unauthenticated") {
    return `/login?next=${encodeURIComponent(currentPath)}`;
  }
  // A setup-only session (ADR 0013): the setup code was accepted, the passkey is still missing.
  if (code === "setup_incomplete") return "/login?setup=passkey";
  return null;
}

/** The `?next=` value to return to after sign-in: a path on this site, otherwise home. */
export function safeNext(next: string | null): string {
  if (next && /^\/(?![/\\])/.test(next)) return next;
  return "/";
}

/**
 * The app's query client. A failed query or mutation that means "signed out" (for example a 401
 * when the session expires mid-use) clears the whole cache, so nothing from the old session
 * survives, then calls `navigate`. API errors aren't retried: they won't change on a retry.
 */
export function createSessionQueryClient(
  navigate: (to: string) => void,
  currentPath: () => string = () =>
    window.location.pathname + window.location.search,
): QueryClient {
  const onError = (error: unknown) => {
    const path = currentPath();
    // Already on the sign-in screen: nothing to redirect (its own /me check fails the same way).
    if (path.startsWith("/login")) return;
    const to = signedOutRedirect(error, path);
    if (to === null) return;
    client.clear();
    navigate(to);
  };
  const client: QueryClient = new QueryClient({
    queryCache: new QueryCache({ onError }),
    mutationCache: new MutationCache({ onError }),
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          !(error instanceof ApiError) && failureCount < 2,
      },
    },
  });
  return client;
}
