/** A non-2xx response. `body` is the parsed JSON (for example a problem detail), the text, or null. */
export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, body: unknown) {
    super(`API request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }

  /** The `error` code from a problem detail, such as "setup_closed". */
  get code(): string | undefined {
    const body = this.body;
    if (
      body &&
      typeof body === "object" &&
      "error" in body &&
      typeof body.error === "string"
    ) {
      return body.error;
    }
    return undefined;
  }
}

/** A 401: not signed in, the session expired, or (on sign-in endpoints) the credential was refused. */
export class UnauthorizedError extends ApiError {
  constructor(body: unknown) {
    super(401, body);
    this.name = "UnauthorizedError";
  }
}

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

function readCookie(name: string): string | undefined {
  for (const pair of document.cookie.split("; ")) {
    const [key, ...rest] = pair.split("=");
    if (key === name) return decodeURIComponent(rest.join("="));
  }
  return undefined;
}

async function readBody(response: Response): Promise<unknown> {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  const type = response.headers.get("Content-Type") ?? "";
  return type.includes("json") ? JSON.parse(text) : text;
}

/**
 * Calls the API with the session cookie. Spring Security sets the XSRF-TOKEN cookie on every
 * response (csrf.spa()), and this sends it back as X-XSRF-TOKEN on state-changing requests.
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (typeof init.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (!SAFE_METHODS.has(method)) {
    const token = readCookie("XSRF-TOKEN");
    if (token) headers.set("X-XSRF-TOKEN", token);
  }

  const response = await fetch(path, {
    ...init,
    method,
    headers,
    credentials: "same-origin",
  });
  const body = await readBody(response);
  if (response.status === 401) throw new UnauthorizedError(body);
  if (!response.ok) throw new ApiError(response.status, body);
  return body as T;
}
