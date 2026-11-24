import { QueryCache, QueryClient } from "@tanstack/react-query";
import { ApiError, UnauthorizedError } from "@/lib/api-client";

/**
 * Any query that gets a 401 (for example when the session expires mid-use) calls onUnauthorized,
 * which sends the user to /login. API errors aren't retried: they won't change on a retry.
 */
export function createQueryClient(onUnauthorized: () => void): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => {
        if (error instanceof UnauthorizedError) onUnauthorized();
      },
    }),
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          !(error instanceof ApiError) && failureCount < 2,
      },
    },
  });
}
