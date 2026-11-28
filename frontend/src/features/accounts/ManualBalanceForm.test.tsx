import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Account } from "@/features/accounts/api";
import { ManualBalanceForm } from "@/features/accounts/ManualBalanceForm";

const account = (balance: string): Account => ({
  id: "a1",
  source: "manual",
  name: "Roth IRA",
  institutionName: "Fidelity",
  kind: "retirement",
  currency: "USD",
  balance,
  balanceAsOf: "2026-11-28",
  updateReminder: "monthly",
  hidden: false,
  needsUpdate: false,
  canDelete: true,
});

let bodies: unknown[] = [];

beforeEach(() => {
  bodies = [];
  document.cookie = "XSRF-TOKEN=test-token";
  vi.stubGlobal(
    "fetch",
    vi.fn(async (_input: string, init: RequestInit = {}) => {
      bodies.push(JSON.parse(String(init.body)));
      return Response.json(account("0.00"));
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("ManualBalanceForm", () => {
  // Bug: the input kept its first value, so Update could revert a newer balance.
  it("showsTheNewBalanceAfterItChangesElsewhere", async () => {
    const client = new QueryClient();
    const wrap = (node: ReactNode) => (
      <QueryClientProvider client={client}>{node}</QueryClientProvider>
    );
    const { rerender } = render(
      wrap(<ManualBalanceForm account={account("8950.00")} />),
    );
    expect(screen.getByLabelText(/Balance today/)).toHaveValue("8,950");

    rerender(wrap(<ManualBalanceForm account={account("9100.50")} />));

    expect(screen.getByLabelText(/Balance today/)).toHaveValue("9,100.50");
    await userEvent.click(screen.getByRole("button", { name: "Update" }));
    await vi.waitFor(() => expect(bodies).toEqual([{ balance: "9100.50" }]));
  });
});
