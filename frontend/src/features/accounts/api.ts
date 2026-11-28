import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { ApiError, apiFetch } from "@/lib/api-client";
import type { components } from "@/lib/api-schema";

export type Account = components["schemas"]["AccountResponse"];
export type AccountHealth = components["schemas"]["AccountHealthResponse"];
export type AccountMatch = components["schemas"]["AccountMatch"];
export type CreateManualAccount =
  components["schemas"]["CreateManualAccountRequest"];
export type UpdateAccount = components["schemas"]["UpdateAccountRequest"];
type ApiProblem = components["schemas"]["ApiProblem"];

export const accountsQueryKey = ["accounts"] as const;
export const accountHealthQueryKey = ["accounts", "health"] as const;

const BASE = "/api/v1/accounts";

const json = (method: string, body: unknown): RequestInit => ({
  method,
  body: JSON.stringify(body),
});

/** Every change to an account can change the list and the health line. */
function invalidateAccounts(client: QueryClient) {
  return Promise.all([
    client.invalidateQueries({ queryKey: accountsQueryKey }),
    client.invalidateQueries({ queryKey: accountHealthQueryKey }),
  ]);
}

export function useAccounts() {
  return useQuery({
    queryKey: accountsQueryKey,
    queryFn: () => apiFetch<Account[]>(BASE),
  });
}

/** "{n} up to date · {m} need attention", shown in the sidebar and the phone menu. */
export function useAccountHealth() {
  return useQuery({
    queryKey: accountHealthQueryKey,
    queryFn: () => apiFetch<AccountHealth>(`${BASE}/health`),
  });
}

/** Existing accounts that look like the one about to be created. Reads only; changes nothing. */
export function useFindMatches() {
  return useMutation({
    mutationFn: (request: CreateManualAccount) =>
      apiFetch<AccountMatch[]>(`${BASE}/matches`, json("POST", request)),
  });
}

function useAccountMutation<V>(mutationFn: (variables: V) => Promise<unknown>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => invalidateAccounts(client),
  });
}

export function useCreateManualAccount() {
  return useAccountMutation((request: CreateManualAccount) =>
    apiFetch<Account>(`${BASE}/manual`, json("POST", request)),
  );
}

export function useSetBalance() {
  return useAccountMutation(
    ({ id, balance }: { id: string; balance: string }) =>
      apiFetch<Account>(`${BASE}/${id}/balance`, json("PUT", { balance })),
  );
}

/** A null field is left alone; a blank apr or minimumPayment clears it. */
export function useUpdateAccount() {
  return useAccountMutation(
    ({ id, changes }: { id: string; changes: UpdateAccount }) =>
      apiFetch<Account>(`${BASE}/${id}`, json("PATCH", changes)),
  );
}

export function useCloseAccount() {
  return useAccountMutation(
    ({ id, closedOn }: { id: string; closedOn: string }) =>
      apiFetch<Account>(`${BASE}/${id}/close`, json("POST", { closedOn })),
  );
}

export function useReopenAccount() {
  return useAccountMutation((id: string) =>
    apiFetch<Account>(`${BASE}/${id}/reopen`, { method: "POST" }),
  );
}

export function useDeleteAccount() {
  return useAccountMutation((id: string) =>
    apiFetch<null>(`${BASE}/${id}`, { method: "DELETE" }),
  );
}

/** Field errors from a `validation_failed` problem, keyed by field name ("balance", "apr"...). */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || error.code !== "validation_failed") {
    return {};
  }
  const errors = (error.body as Partial<ApiProblem> | null)?.errors ?? [];
  return Object.fromEntries(errors.map((e) => [e.field, e.message]));
}

/**
 * The backend's DuplicateAccountMatcher.normalize: strip, collapse inner whitespace, lowercase.
 * Java's \s (without the Unicode flag) is only these six characters, so the class is spelled out.
 */
export function normalizeInstitution(value: string | null | undefined): string {
  if (value == null) return "";
  return value
    .trim()
    .replace(/[ \t\n\v\f\r]+/g, " ")
    .toLowerCase();
}

export type AccountGroup = {
  /** Stable React key: the normalized institution, or the account id when there's none. */
  key: string;
  /** The institution as the first account spells it, or the account's name. */
  title: string;
  /** True when the card is titled with the account's own name (no institution). */
  titledByAccount: boolean;
  accounts: Account[];
};

const isClosed = (account: Account) => account.closedOn != null;

/**
 * One card per institution (grouped the way the backend matches duplicates); an account with no
 * institution gets its own card. Closed accounts sort last, inside a card and across cards.
 */
export function groupAccounts(accounts: Account[]): AccountGroup[] {
  const groups = new Map<string, AccountGroup>();
  for (const account of accounts) {
    const institution = normalizeInstitution(account.institutionName);
    const key = institution === "" ? `account:${account.id}` : institution;
    const group = groups.get(key);
    if (group) {
      group.accounts.push(account);
    } else {
      groups.set(key, {
        key,
        title:
          institution === ""
            ? (account.name ?? "")
            : (account.institutionName ?? "").trim(),
        titledByAccount: institution === "",
        accounts: [account],
      });
    }
  }
  const byName = (a: string, b: string) =>
    a.localeCompare(b, undefined, { sensitivity: "base" });
  const result = [...groups.values()];
  for (const group of result) {
    group.accounts.sort(
      (a, b) =>
        Number(isClosed(a)) - Number(isClosed(b)) ||
        byName(a.name ?? "", b.name ?? ""),
    );
  }
  const allClosed = (group: AccountGroup) => group.accounts.every(isClosed);
  return result.sort(
    (a, b) =>
      Number(allClosed(a)) - Number(allClosed(b)) || byName(a.title, b.title),
  );
}
