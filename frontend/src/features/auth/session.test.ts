import { describe, expect, it, vi } from "vitest";
import {
  createSessionQueryClient,
  safeNext,
  signedOutRedirect,
} from "@/features/auth/session";
import { ApiError, UnauthorizedError } from "@/lib/api-client";

describe("signedOutRedirect", () => {
  it("sendsUnauthenticatedToLoginWithNext", () => {
    const error = new UnauthorizedError({ error: "unauthenticated" });
    expect(signedOutRedirect(error, "/plan?tab=1")).toBe(
      "/login?next=%2Fplan%3Ftab%3D1",
    );
  });

  it("sendsSetupIncompleteToThePasskeyStep", () => {
    const error = new ApiError(403, { error: "setup_incomplete" });
    expect(signedOutRedirect(error, "/")).toBe("/login?setup=passkey");
  });

  it("ignoresForbiddenAndCodelessErrors", () => {
    expect(
      signedOutRedirect(new ApiError(403, { error: "forbidden" }), "/"),
    ).toBeNull();
    expect(signedOutRedirect(new ApiError(403, null), "/")).toBeNull();
    expect(signedOutRedirect(new Error("boom"), "/")).toBeNull();
  });
});

describe("safeNext", () => {
  it("keepsOnlySameSitePaths", () => {
    expect(safeNext("/settings")).toBe("/settings");
    expect(safeNext("//evil.example")).toBe("/");
    expect(safeNext("https://evil.example")).toBe("/");
    expect(safeNext("/\\evil.example")).toBe("/");
    expect(safeNext(null)).toBe("/");
    expect(safeNext("/\t/evil.example")).toBe("/");
    expect(safeNext("/\n/evil.example")).toBe("/");
    expect(safeNext("/\r/evil.example")).toBe("/");
  });
});

describe("createSessionQueryClient", () => {
  const signedOut = () =>
    Promise.reject(new UnauthorizedError({ error: "unauthenticated" }));

  it("skipsTheRedirectOnlyOnTheLoginPageItself", async () => {
    const navigate = vi.fn();
    for (const path of ["/login", "/login?next=%2F"]) {
      const client = createSessionQueryClient(navigate, () => path);
      await client
        .fetchQuery({ queryKey: ["a"], queryFn: signedOut })
        .catch(() => {});
    }
    expect(navigate).not.toHaveBeenCalled();

    const client = createSessionQueryClient(navigate, () => "/loginfoo");
    await client
      .fetchQuery({ queryKey: ["a"], queryFn: signedOut })
      .catch(() => {});
    expect(navigate).toHaveBeenCalledWith("/login?next=%2Floginfoo");
  });
});
