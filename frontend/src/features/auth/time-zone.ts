import { useEffect, useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import { meQueryKey, type Me } from "@/features/auth/use-me";

/** The browser's IANA time zone, such as "America/Los_Angeles". */
export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * Sends the browser's time zone only while none is stored (design.md: set from the browser at first
 * sign-in, then changed in Settings). The zone decides which day a balance belongs to, so opening
 * the app in another zone while travelling never moves it. Tried once per page load: a failure
 * isn't retried in a loop, and the next load tries again.
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
    // null (or absent) means never set; any stored zone is the user's choice.
    if (!me || attempted.current || me.timeZone != null) return;
    const zone = browserTimeZone();
    if (!zone) return;
    attempted.current = true;
    mutate(zone);
  }, [me, mutate]);
}
