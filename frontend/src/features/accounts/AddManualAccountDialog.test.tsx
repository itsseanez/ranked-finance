import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AddManualAccountDialog } from "@/features/accounts/AddManualAccountDialog";

type Call = { method: string; path: string; body: unknown };
type Handler = (call: Call) => Response;

let calls: Call[] = [];

/** Fake API keyed by "METHOD /path"; anything else fails the test. */
function stubFetch(handlers: Record<string, Handler>) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const path = typeof input === "string" ? input : String(input);
      const method = (init.method ?? "GET").toUpperCase();
      const body =
        typeof init.body === "string" ? JSON.parse(init.body) : undefined;
      const call = { method, path, body };
      calls.push(call);
      const handler = handlers[`${method} ${path}`];
      if (!handler) throw new Error(`Unexpected fetch: ${method} ${path}`);
      return handler(call);
    }),
  );
}

const account = (id: string, name: string) => ({
  id,
  source: "manual",
  name,
  institutionName: "Fidelity",
  kind: "retirement",
  currency: "USD",
  balance: "8950.00",
  balanceAsOf: "2026-11-28",
  apr: null,
  minimumPayment: null,
  updateReminder: "monthly",
  hidden: false,
  closedOn: null,
  needsUpdate: false,
  canDelete: true,
});

function renderDialog() {
  const onClose = vi.fn();
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <AddManualAccountDialog onClose={onClose} />
    </QueryClientProvider>,
  );
  return { onClose };
}

async function fillAndSubmit() {
  await userEvent.type(screen.getByLabelText("Account name"), "Roth IRA");
  await userEvent.type(screen.getByLabelText(/Institution/), "Fidelity");
  await userEvent.selectOptions(screen.getByLabelText("Type"), "retirement");
  await userEvent.type(screen.getByLabelText("Balance today"), "$8,950");
  await userEvent.click(screen.getByRole("button", { name: "Add account" }));
}

const posts = (path: string) =>
  calls.filter((c) => c.method !== "GET" && c.path === path);

beforeEach(() => {
  calls = [];
  // The CSRF cookie is already there, so apiFetch doesn't fetch /me first.
  document.cookie = "XSRF-TOKEN=test-token";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("AddManualAccountDialog (duplicate-account matching)", () => {
  const withMatch = (extra: Record<string, Handler> = {}) =>
    stubFetch({
      "POST /api/v1/accounts/matches": () =>
        Response.json([{ id: "a1", name: "Fidelity Roth IRA" }]),
      ...extra,
    });

  it("asksBeforeAddingADuplicate", async () => {
    withMatch();
    renderDialog();

    await fillAndSubmit();

    expect(
      await screen.findByText("Is this the same as Fidelity Roth IRA?"),
    ).toBeInTheDocument();
    expect(posts("/api/v1/accounts/matches")[0].body).toMatchObject({
      name: "Roth IRA",
      institutionName: "Fidelity",
      kind: "retirement",
      balance: "8950.00",
    });
    expect(posts("/api/v1/accounts/manual")).toHaveLength(0);
  });

  it("sameAccountUpdatesTheExistingBalance", async () => {
    withMatch({
      "PUT /api/v1/accounts/a1/balance": () =>
        Response.json(account("a1", "Fidelity Roth IRA")),
    });
    const { onClose } = renderDialog();

    await fillAndSubmit();
    await userEvent.click(await screen.findByRole("button", { name: "Yes" }));

    await vi.waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(posts("/api/v1/accounts/a1/balance")[0].body).toEqual({
      balance: "8950.00",
    });
    expect(posts("/api/v1/accounts/manual")).toHaveLength(0);
  });

  it("differentAccountIsCreated", async () => {
    withMatch({
      "POST /api/v1/accounts/manual": () =>
        Response.json(account("a2", "Roth IRA"), { status: 201 }),
    });
    const { onClose } = renderDialog();

    await fillAndSubmit();
    await userEvent.click(
      await screen.findByRole("button", { name: "No, add it" }),
    );

    await vi.waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(posts("/api/v1/accounts/manual")[0].body).toMatchObject({
      name: "Roth IRA",
      kind: "retirement",
      balance: "8950.00",
    });
    expect(posts("/api/v1/accounts/a1/balance")).toHaveLength(0);
  });
});
