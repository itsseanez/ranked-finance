import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";
import type { components } from "@/lib/api-schema";

export type Me = components["schemas"]["MeResponse"];

export const meQueryKey = ["me"] as const;

/** The signed-in user. A 401 here means "not signed in". */
export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: () => apiFetch<Me>("/api/v1/me"),
  });
}
