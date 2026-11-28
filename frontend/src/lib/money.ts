// An optional "-", an optional "$", whole dollars (plain, or grouped with commas), then up to two
// decimals. Three decimals don't match, so a fraction of a cent is rejected rather than rounded.
const MONEY_INPUT = /^(-)?\$?(\d{1,3}(?:,\d{3})+|\d*)(?:\.(\d{0,2}))?$/;

// numeric(14,2) on the server: at most 12 digits before the point.
const MAX_WHOLE_DIGITS = 12;

/**
 * Turns what someone typed ("8950", "8,950.5", "$8,950.50", "-5") into the API's money format, a
 * plain decimal with two places ("8950.50"), or null if it isn't an amount of money.
 */
export function parseMoneyInput(text: string): string | null {
  const match = MONEY_INPUT.exec(text.trim());
  if (!match) return null;
  const [, sign, wholeText, cents = ""] = match;
  const whole = wholeText.replaceAll(",", "");
  if (whole === "" && cents === "") return null;
  const digits = whole.replace(/^0+(?=\d)/, "") || "0";
  if (digits.length > MAX_WHOLE_DIGITS) return null;
  const value = `${digits}.${cents.padEnd(2, "0")}`;
  return sign && value !== "0.00" ? `-${value}` : value;
}

/**
 * Formats an API amount ("8450.00") for display in the user's locale. Whole amounts drop the cents
 * ("$8,450"), as the mockups show. Debts are stored as positive balances and shown with a minus.
 */
export function formatMoney(
  amount: string,
  currency: string,
  { debt = false }: { debt?: boolean } = {},
): string {
  const value = Number(amount);
  const wholeDollars = Number.isInteger(value);
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency,
    minimumFractionDigits: wholeDollars ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(debt && value !== 0 ? -Math.abs(value) : value);
}
