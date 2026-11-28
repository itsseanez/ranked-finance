import { useEffect, useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { meQueryKey, type Me } from "@/features/auth/use-me";

/** The browser's IANA time zone, such as "America/Los_Angeles". */
export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * Sends the browser's time zone once when it differs from the stored one (design.md: set from the
 * browser at first sign-in). The zone decides which day a balance belongs to. Tried once per page
 * load: a failure isn't retried in a loop, and the next load tries again.
 */
export function useSyncTimeZone(me: Me | undefined) {
  const queryClient = useQueryClient();
  const attempted = useRef(false);
  const { mutate } = useMutation({
    mutationFn: (timeZone: string) =>
      apiFetch<void>("/api/v1/me/time-zone", {
        method: "PUT",
        body: JSON.stringify({ timeZone }),
      }),
    onSuccess: (_, timeZone) => {
      queryClient.setQueryData<Me>(meQueryKey, (old) =>
        old ? { ...old, timeZone } : old,
      );
    },
  });

  useEffect(() => {
    if (!me || attempted.current) return;
    const zone = browserTimeZone();
    if (!zone || zone === me.timeZone) return;
    attempted.current = true;
    mutate(zone);
  }, [me, mutate]);
}
