import { createBrowserRouter } from "react-router";
import { AppShell } from "@/app/shell/AppShell";
import { HomePage } from "@/features/home/HomePage";
import { MoneyPage } from "@/features/money/MoneyPage";
import { NotificationsPage } from "@/features/notifications/NotificationsPage";
import { PlanPage } from "@/features/plan/PlanPage";
import { RanksPage } from "@/features/ranks/RanksPage";
import { SettingsPage } from "@/features/settings/SettingsPage";

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "plan", element: <PlanPage /> },
      { path: "money", element: <MoneyPage /> },
      { path: "ranks", element: <RanksPage /> },
      { path: "settings", element: <SettingsPage /> },
      { path: "notifications", element: <NotificationsPage /> },
    ],
  },
]);
