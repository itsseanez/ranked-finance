import { MutationObserver } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { createMemoryRouter } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppProviders } from "@/app/App";
import { routes } from "@/app/router";
import { createSessionQueryClient } from "@/features/auth/session";
import { apiFetch } from "@/lib/api-client";

type Responses = Record<string, () => Response>;

const signedIn = () =>
  Response.json({ id: "1", displayName: "Owner", email: "owner@example.com" });
const problem = (code: number, error: string) => () =>
  new Response(JSON.stringify({ status: code, error }), {
    status: code,
    headers: { "Content-Type": "application/problem+json" },
  });

/** Renders the whole app at `path`, answering API calls from `responses` (path → response). */
function renderApp(path: string, responses: Responses) {
  // jsdom has no matchMedia; the shell uses it to pick the desktop layout.
  vi.stubGlobal("matchMedia", (query: string) => ({
    matches: true,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }));
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: string) => {
      const respond = responses[input];
      if (!respond) throw new Error(`Unexpected fetch: ${input}`);
      return respond();
    }),
  );
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  const queryClient = createSessionQueryClient(
    (to) => void router.navigate(to, { replace: true }),
    () => router.state.location.pathname + router.state.location.search,
  );
  render(<AppProviders router={router} queryClient={queryClient} />);
  return { router, queryClient };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("auth gate", () => {
  it("redirectsToLoginOn401", async () => {
    const { router } = renderApp("/", {
      "/api/v1/me": problem(401, "unauthenticated"),
    });

    expect(
      await screen.findByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
  });

  // The session expires mid-use: any query that gets a 401 sends the user to /login, not just /me.
  it("redirectsToLoginWhenAnyQueryGets401", async () => {
    let sessionExpired = false;
    const { router, queryClient } = renderApp("/", {
      "/api/v1/me": () =>
        sessionExpired ? problem(401, "unauthenticated")() : signedIn(),
      "/api/v1/other": () => {
        sessionExpired = true;
        return problem(401, "unauthenticated")();
      },
    });
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

  it("redirectsHomeWhenAlreadySignedIn", async () => {
    const { router } = renderApp("/login", { "/api/v1/me": signedIn });

    expect(
      await screen.findByRole("heading", { name: "Home" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
  });

  it("redirectsToLoginWithNextOn401", async () => {
    const { router } = renderApp("/settings", {
      "/api/v1/me": problem(401, "unauthenticated"),
    });

    expect(
      await screen.findByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
    expect(router.state.location.search).toBe("?next=%2Fsettings");
  });

  it("redirectsWhenAMutationGets401", async () => {
    let sessionExpired = false;
    const { router, queryClient } = renderApp("/", {
      "/api/v1/me": () =>
        sessionExpired ? problem(401, "unauthenticated")() : signedIn(),
      "/api/v1/things": () => {
        sessionExpired = true;
        return problem(401, "unauthenticated")();
      },
    });
    await screen.findByRole("heading", { name: "Home" });

    await new MutationObserver(queryClient, {
      mutationFn: () => apiFetch("/api/v1/things", { method: "DELETE" }),
    })
      .mutate()
      .catch(() => {});

    expect(
      await screen.findByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
  });

  it("forgetsCachedDataWhenSignedOut", async () => {
    let sessionExpired = false;
    const { queryClient } = renderApp("/", {
      "/api/v1/me": () =>
        sessionExpired ? problem(401, "unauthenticated")() : signedIn(),
      "/api/v1/other": () => {
        sessionExpired = true;
        return problem(401, "unauthenticated")();
      },
    });
    await screen.findByRole("heading", { name: "Home" });
    queryClient.setQueryData(["accounts"], [{ id: "a1" }]);

    await queryClient
      .fetchQuery({
        queryKey: ["other"],
        queryFn: () => apiFetch("/api/v1/other"),
      })
      .catch(() => {});
    await screen.findByRole("button", { name: "Continue with passkey" });

    expect(queryClient.getQueryData(["accounts"])).toBeUndefined();
    expect(queryClient.getQueryData(["me"])).toBeUndefined();
  });

  // A setup-only session (passkey setup didn't finish) can't use the app. The setup code was
  // already accepted, so login goes straight to creating the passkey.
  it("setupSessionGoesToThePasskeyStep", async () => {
    const { router } = renderApp("/", {
      "/api/v1/me": problem(403, "setup_incomplete"),
    });

    expect(
      await screen.findByRole("button", { name: "Create your passkey" }),
    ).toBeInTheDocument();
    expect(router.state.location.search).toBe("?setup=passkey");
  });

  it("forbiddenNeverRedirects", async () => {
    const { router } = renderApp("/", {
      "/api/v1/me": problem(403, "forbidden"),
    });

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
  });
});
