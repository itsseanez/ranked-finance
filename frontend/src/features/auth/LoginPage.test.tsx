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

function renderLoginPage(entry = "/login") {
  const router = createMemoryRouter(
    [
      { path: "/login", element: <LoginPage /> },
      { path: "/", element: <p>Home</p> },
      { path: "/settings", element: <p>Settings</p> },
    ],
    { initialEntries: [entry] },
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

  it("showsOwnerMismatchMessage", async () => {
    stubFetch({ "/api/v1/setup": () => problem(409, "owner_mismatch") });
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
        "This server is set up for a different owner email. Check OWNER_EMAIL.",
      ),
    ).toBeInTheDocument();
  });

  // Older browsers have WebAuthn but not the JSON helpers this app relies on.
  it("showsUnsupportedMessageWithoutJsonHelpers", async () => {
    vi.stubGlobal("PublicKeyCredential", {});
    stubFetch({});
    renderLoginPage();

    await userEvent.click(
      screen.getByRole("button", { name: "Continue with passkey" }),
    );

    expect(
      await screen.findByText(
        "This browser doesn't support passkeys. Try the latest Chrome, Safari, or Edge.",
      ),
    ).toBeInTheDocument();
  });

  // The setup code only unlocks passkey registration (ADR 0013); the user then signs in with it.
  it("signsInWithTheNewPasskeyAfterSetup", async () => {
    const credential = { toJSON: () => ({ id: "new-passkey" }) };
    stubFetch({
      "/api/v1/setup": () => new Response(null, { status: 204 }),
      "/webauthn/register/options": () => Response.json({ challenge: "abc" }),
      "/webauthn/register": () => Response.json({ success: true }),
      "/webauthn/authenticate/options": () =>
        Response.json({ challenge: "def" }),
      "/login/webauthn": () =>
        Response.json({ authenticated: true, redirectUrl: "/" }),
    });
    stubCredentials({
      create: vi.fn().mockResolvedValue(credential),
      get: vi.fn().mockResolvedValue(credential),
    });
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

    expect(await screen.findByText("Home")).toBeInTheDocument();
    const paths = vi.mocked(fetch).mock.calls.map(([input]) => String(input));
    expect(paths.filter((path) => path !== "/api/v1/me")).toEqual([
      "/api/v1/setup",
      "/webauthn/register/options",
      "/webauthn/register",
      "/webauthn/authenticate/options",
      "/login/webauthn",
    ]);
  });

  // ADR 0013: if the sign-in right after setup fails, the passkey still exists, so the sign-in
  // screen is the way back in.
  it("offersSignInWhenSignInAfterSetupFails", async () => {
    stubFetch({
      "/api/v1/setup": () => new Response(null, { status: 204 }),
      "/webauthn/register/options": () => Response.json({ challenge: "abc" }),
      "/webauthn/register": () => Response.json({ success: true }),
      "/webauthn/authenticate/options": () =>
        Response.json({ challenge: "def" }),
    });
    stubCredentials({
      create: vi
        .fn()
        .mockResolvedValue({ toJSON: () => ({ id: "new-passkey" }) }),
      get: vi.fn().mockRejectedValue(new DOMException("", "NotAllowedError")),
    });
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
        "Your passkey is set up. Sign-in was cancelled. Try again.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Continue with passkey" }),
    ).toBeInTheDocument();
  });

  describe("after sign-in", () => {
    const signInHandlers = () => {
      const credential = { toJSON: () => ({ id: "passkey" }) };
      stubFetch({
        "/webauthn/authenticate/options": () =>
          Response.json({ challenge: "abc" }),
        "/login/webauthn": () =>
          Response.json({ authenticated: true, redirectUrl: "/" }),
      });
      stubCredentials({ get: vi.fn().mockResolvedValue(credential) });
    };

    it("returnsToNextAfterSignIn", async () => {
      signInHandlers();
      renderLoginPage("/login?next=%2Fsettings");

      await userEvent.click(
        screen.getByRole("button", { name: "Continue with passkey" }),
      );

      expect(await screen.findByText("Settings")).toBeInTheDocument();
    });

    it("ignoresAnOffSiteNext", async () => {
      signInHandlers();
      renderLoginPage("/login?next=%2F%2Fevil.example");

      await userEvent.click(
        screen.getByRole("button", { name: "Continue with passkey" }),
      );

      expect(await screen.findByText("Home")).toBeInTheDocument();
    });
  });
});
