import { render, screen } from "@testing-library/react";
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
});
