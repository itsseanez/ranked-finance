import type { ReactNode } from "react";
import { Navigate } from "react-router";
import { useMe } from "@/features/auth/use-me";
import { isSignedOut } from "@/lib/api-client";

/** Renders its children only for a signed-in user; everyone else goes to /login. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const me = useMe();

  if (me.isPending) return null;
  if (me.isError) {
    if (isSignedOut(me.error)) return <Navigate to="/login" replace />;
    return (
      <p role="alert" className="p-8">
        Couldn't load your account. Reload the page to try again.
      </p>
    );
  }
  return children;
}
