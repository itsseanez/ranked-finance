import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router";
import { useMe } from "@/features/auth/use-me";
import { signedOutRedirect } from "@/features/auth/session";
import { useSyncTimeZone } from "@/features/auth/time-zone";

/** Renders its children only for a signed-in user; everyone else goes to /login. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const me = useMe();
  const location = useLocation();
  useSyncTimeZone(me.data);

  if (me.isPending) return null;
  if (me.isError) {
    const to = signedOutRedirect(me.error, location.pathname + location.search);
    if (to) return <Navigate to={to} replace />;
    return (
      <p role="alert" className="p-8">
        Couldn't load your account. Reload the page to try again.
      </p>
    );
  }
  return children;
}
