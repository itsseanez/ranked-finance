import { type QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ComponentProps } from "react";
import { RouterProvider } from "react-router/dom";
import { createQueryClient } from "@/app/query-client";
import { router as appRouter } from "@/app/router";

type AppRouter = ComponentProps<typeof RouterProvider>["router"];

const appQueryClient = createQueryClient(() => {
  if (appRouter.state.location.pathname !== "/login") {
    void appRouter.navigate("/login", { replace: true });
  }
});

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
