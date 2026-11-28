import { describe, expect, it } from "vitest";
import { formatMoney, parseMoneyInput } from "@/lib/money";

describe("parseMoneyInput", () => {
  it("parsesDollarsAndCents", () => {
    expect(parseMoneyInput("8950")).toBe("8950.00");
    expect(parseMoneyInput("8,950.5")).toBe("8950.50");
    expect(parseMoneyInput("$8,950.50")).toBe("8950.50");
    expect(parseMoneyInput("-5")).toBe("-5.00");
    expect(parseMoneyInput("-$5")).toBe("-5.00");
    expect(parseMoneyInput("  1,234  ")).toBe("1234.00");
    // The backend rejects "5." and ".5", so they're normalized here.
    expect(parseMoneyInput("5.")).toBe("5.00");
    expect(parseMoneyInput(".5")).toBe("0.50");
    expect(parseMoneyInput("0")).toBe("0.00");
  });

  it("rejectsFractionsOfACent", () => {
    expect(parseMoneyInput("1.005")).toBeNull();
    expect(parseMoneyInput("$8,950.501")).toBeNull();
  });

  it("rejectsText", () => {
    expect(parseMoneyInput("")).toBeNull();
    expect(parseMoneyInput("abc")).toBeNull();
    expect(parseMoneyInput("12abc")).toBeNull();
    expect(parseMoneyInput("1e5")).toBeNull();
    expect(parseMoneyInput("$")).toBeNull();
    expect(parseMoneyInput("-")).toBeNull();
    expect(parseMoneyInput(".")).toBeNull();
    expect(parseMoneyInput("1.2.3")).toBeNull();
    expect(parseMoneyInput("--5")).toBeNull();
    // More digits than the backend's numeric(14,2) can hold.
    expect(parseMoneyInput("1234567890123")).toBeNull();
  });
});

describe("formatMoney", () => {
  it("showsDebtsWithAMinusSign", () => {
    const debt = formatMoney("8450.00", "USD", { debt: true });
    expect(debt).toMatch(/[-−]/);
    expect(debt).toContain("8,450");
    expect(formatMoney("8200.00", "USD")).not.toMatch(/[-−]/);
  });
});
