import type { ReactNode } from "react";
import { Navigate } from "react-router";
import { useMe } from "@/features/auth/use-me";
import { ApiError, UnauthorizedError } from "@/lib/api-client";

/** Renders its children only for a signed-in user; everyone else goes to /login. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const me = useMe();

  if (me.isPending) return null;
  if (me.isError) {
    // 403: a setup-only session, where passkey setup didn't finish (ADR 0013). Either way, sign in.
    const signedOut =
      me.error instanceof UnauthorizedError ||
      (me.error instanceof ApiError && me.error.status === 403);
    if (signedOut) return <Navigate to="/login" replace />;
    return (
      <p role="alert" className="p-8">
        Couldn't load your account. Reload the page to try again.
      </p>
    );
  }
  return children;
}
