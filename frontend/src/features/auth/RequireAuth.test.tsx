import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RequireAuth } from "@/features/auth/RequireAuth";

// design.md: the time zone is set from the browser at first sign-in, then only changed in
// Settings. It decides which day a balance belongs to, so opening the app while travelling must
// not move it. A null stored zone means it was never set.
function stubServer(storedZone: string | null) {
  document.cookie = "XSRF-TOKEN=test-token";
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = typeof input === "string" ? input : input.toString();
      if (path === "/api/v1/me") {
        return new Response(
          JSON.stringify({
            id: "00000000-0000-0000-0000-000000000001",
            displayName: "Owner",
            email: "owner@example.com",
            timeZone: storedZone,
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (path === "/api/v1/me/time-zone" && init?.method === "PUT") {
        return new Response(null, { status: 204 });
      }
      throw new Error(`Unexpected fetch: ${init?.method ?? "GET"} ${path}`);
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function stubBrowserZone(zone: string) {
  const real = Intl.DateTimeFormat.prototype.resolvedOptions;
  vi.spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions").mockImplementation(
    function (this: Intl.DateTimeFormat) {
      return { ...real.call(this), timeZone: zone };
    },
  );
}

function timeZonePuts(fetchMock: ReturnType<typeof stubServer>) {
  return fetchMock.mock.calls.filter(
    ([path, init]) => path === "/api/v1/me/time-zone" && init?.method === "PUT",
  );
}

function renderProtectedPage() {
  const router = createMemoryRouter(
    [
      {
        path: "/",
        element: (
          <RequireAuth>
            <p>Home</p>
          </RequireAuth>
        ),
      },
    ],
    { initialEntries: ["/"] },
  );
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe("RequireAuth time zone", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("sendsTheBrowserZoneOnFirstSignIn", async () => {
    const fetchMock = stubServer(null);
    stubBrowserZone("America/Los_Angeles");

    renderProtectedPage();

    await screen.findByText("Home");
    await waitFor(() => expect(timeZonePuts(fetchMock)).toHaveLength(1));
    const [, init] = timeZonePuts(fetchMock)[0];
    expect(JSON.parse(init?.body as string)).toEqual({
      timeZone: "America/Los_Angeles",
    });
  });

  it("neverOverwritesAStoredZone", async () => {
    const fetchMock = stubServer("America/New_York");
    stubBrowserZone("America/Los_Angeles");

    renderProtectedPage();

    await screen.findByText("Home");
    // Give a stray effect a chance to fire before asserting it didn't.
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(timeZonePuts(fetchMock)).toHaveLength(0);
  });
});
