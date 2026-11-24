import { render, screen } from "@testing-library/react";
import { apiFetch } from "@/lib/api-client";
import { createMemoryRouter } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppProviders } from "@/app/App";
import { createQueryClient } from "@/app/query-client";
import { routes } from "@/app/router";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("auth gate", () => {
  it("redirectsToLoginOn401", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string) => {
        if (path === "/api/v1/me") return new Response(null, { status: 401 });
        throw new Error(`Unexpected fetch: ${path}`);
      }),
    );
    const router = createMemoryRouter(routes, { initialEntries: ["/"] });
    const queryClient = createQueryClient(
      () => void router.navigate("/login", { replace: true }),
    );

    render(<AppProviders router={router} queryClient={queryClient} />);

    expect(
      await screen.findByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
  });

  // The session expires mid-use: any query that gets a 401 sends the user to /login, not just /me.
  it("redirectsToLoginWhenAnyQueryGets401", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string) => {
        if (path === "/api/v1/me") {
          return Response.json({
            id: "1",
            displayName: "Owner",
            email: "owner@example.com",
          });
        }
        if (path === "/api/v1/other")
          return new Response(null, { status: 401 });
        throw new Error(`Unexpected fetch: ${path}`);
      }),
    );
    // jsdom has no matchMedia; the shell uses it to pick the desktop layout.
    vi.stubGlobal("matchMedia", (query: string) => ({
      matches: true,
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    }));
    const router = createMemoryRouter(routes, { initialEntries: ["/"] });
    const queryClient = createQueryClient(
      () => void router.navigate("/login", { replace: true }),
    );
    render(<AppProviders router={router} queryClient={queryClient} />);
    await screen.findByRole("heading", { name: "Home" });

    await queryClient
      .fetchQuery({
        queryKey: ["other"],
        queryFn: () => apiFetch("/api/v1/other"),
      })
      .catch(() => {});

    expect(
      await screen.findByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
  });
});
