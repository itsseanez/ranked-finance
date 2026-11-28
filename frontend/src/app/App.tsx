import { type QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ComponentProps } from "react";
import { RouterProvider } from "react-router/dom";
import { router as appRouter } from "@/app/router";
import { createSessionQueryClient } from "@/features/auth/session";

type AppRouter = ComponentProps<typeof RouterProvider>["router"];

const appQueryClient = createSessionQueryClient(
  (to) => void appRouter.navigate(to, { replace: true }),
  () => appRouter.state.location.pathname + appRouter.state.location.search,
);

export function AppProviders({
  router,
  queryClient,
}: {
  router: AppRouter;
  queryClient: QueryClient;
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}

export function App() {
  return <AppProviders router={appRouter} queryClient={appQueryClient} />;
}
