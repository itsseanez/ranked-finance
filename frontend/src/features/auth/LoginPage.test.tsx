import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "@/features/auth/LoginPage";

type Handler = () => Response;

function stubFetch(handlers: Record<string, Handler>) {
  const all: Record<string, Handler> = {
    // Like the real server: signed out, so 401, and the response sets the CSRF cookie.
    "/api/v1/me": () => {
      document.cookie = "XSRF-TOKEN=test-token";
      return new Response(null, { status: 401 });
    },
    ...handlers,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL) => {
      const path =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.pathname
            : input.url;
      const handler = all[path];
      if (!handler) throw new Error(`Unexpected fetch: ${path}`);
      return handler();
    }),
  );
}

function problem(status: number, error: string): Response {
  return new Response(JSON.stringify({ status, error }), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

function renderLoginPage() {
  const router = createMemoryRouter(
    [
      { path: "/login", element: <LoginPage /> },
      { path: "/", element: <p>Home</p> },
    ],
    { initialEntries: ["/login"] },
  );
  render(
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

function stubCredentials(credentials: Partial<CredentialsContainer>) {
  Object.defineProperty(navigator, "credentials", {
    value: credentials,
    configurable: true,
  });
}

beforeEach(() => {
  // jsdom has no WebAuthn. The parse helpers just pass the options through.
  vi.stubGlobal("PublicKeyCredential", {
    parseRequestOptionsFromJSON: (options: unknown) => options,
    parseCreationOptionsFromJSON: (options: unknown) => options,
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  Reflect.deleteProperty(navigator, "credentials");
});

describe("LoginPage", () => {
  it("showsMessageWhenPasskeyCancelled", async () => {
    stubFetch({
      "/webauthn/authenticate/options": () =>
        Response.json({ challenge: "abc" }),
    });
    stubCredentials({
      get: vi.fn().mockRejectedValue(new DOMException("", "NotAllowedError")),
    });
    renderLoginPage();

    await userEvent.click(
      screen.getByRole("button", { name: "Continue with passkey" }),
    );

    expect(
      await screen.findByText("Sign-in was cancelled. Try again."),
    ).toBeInTheDocument();
  });

  it("showsSetupClosedMessage", async () => {
    stubFetch({ "/api/v1/setup": () => problem(403, "setup_closed") });
    renderLoginPage();

    await userEvent.click(
      screen.getByRole("button", { name: "First-time setup" }),
    );
    await userEvent.type(
      screen.getByLabelText("Setup code"),
      "some-setup-code-1234567",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Create passkey" }),
    );

    expect(
      await screen.findByText(
        "This app is already set up. Sign in with your passkey.",
      ),
    ).toBeInTheDocument();
  });
});
