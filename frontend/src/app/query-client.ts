import { QueryCache, QueryClient } from "@tanstack/react-query";
import { meQueryKey } from "@/features/auth/use-me";
import { ApiError, isSignedOut } from "@/lib/api-client";

/**
 * Any query whose error means signed out (for example a 401 when the session expires mid-use) calls
 * onUnauthorized,
 * which sends the user to /login. API errors aren't retried: they won't change on a retry.
 */
export function createQueryClient(onUnauthorized: () => void): QueryClient {
  const client: QueryClient = new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => {
        if (isSignedOut(error)) {
          // The session is gone: forget who was signed in, or /login would bounce back home.
          client.removeQueries({ queryKey: meQueryKey });
          onUnauthorized();
        }
      },
    }),
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          !(error instanceof ApiError) && failureCount < 2,
      },
    },
  });
  return client;
}
