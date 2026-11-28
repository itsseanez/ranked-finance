import { createBrowserRouter, type RouteObject } from "react-router";
import { SettingsAccountsPage } from "@/app/pages/SettingsAccountsPage";
import { AppShell } from "@/app/shell/AppShell";
import { LoginPage } from "@/features/auth/LoginPage";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { HomePage } from "@/features/home/HomePage";
import { MoneyPage } from "@/features/money/MoneyPage";
import { NotificationsPage } from "@/features/notifications/NotificationsPage";
import { PlanPage } from "@/features/plan/PlanPage";
import { RanksPage } from "@/features/ranks/RanksPage";

export const routes: RouteObject[] = [
  { path: "login", element: <LoginPage /> },
  {
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <HomePage /> },
      { path: "plan", element: <PlanPage /> },
      { path: "money", element: <MoneyPage /> },
      { path: "ranks", element: <RanksPage /> },
      { path: "settings", element: <SettingsAccountsPage /> },
      { path: "notifications", element: <NotificationsPage /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
