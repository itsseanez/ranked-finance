import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api-client";
import { errorMessages, messageFor } from "@/lib/errors";

describe("messageFor", () => {
  it("usesTheMessageForAKnownCode", () => {
    const error = new ApiError(403, { status: 403, error: "setup_closed" });
    expect(messageFor(error)).toBe(errorMessages.setup_closed);
  });

  // ADR 0014: the server may add codes this build doesn't know yet.
  it("unknownCodeFallsBackToAGenericMessage", () => {
    const error = new ApiError(409, { status: 409, error: "added_later" });
    expect(messageFor(error)).toBe("Something went wrong. Try again.");
    expect(messageFor(new Error("boom"))).toBe(
      "Something went wrong. Try again.",
    );
  });
});
