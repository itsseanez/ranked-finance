import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch, UnauthorizedError } from "@/lib/api-client";

function stubFetch(response: Response) {
  const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("apiFetch", () => {
  it("addsCsrfHeaderOnPost", async () => {
    document.cookie = "XSRF-TOKEN=abc";
    const fetchMock = stubFetch(new Response(null, { status: 204 }));

    await apiFetch("/api/v1/setup", {
      method: "POST",
      body: JSON.stringify({ code: "x" }),
    });

    const init = fetchMock.mock.calls[0][1];
    expect(new Headers(init?.headers).get("X-XSRF-TOKEN")).toBe("abc");
  });

  it("throwsUnauthorizedOn401", async () => {
    stubFetch(new Response(null, { status: 401 }));

    await expect(apiFetch("/api/v1/me")).rejects.toBeInstanceOf(
      UnauthorizedError,
    );
  });
});
