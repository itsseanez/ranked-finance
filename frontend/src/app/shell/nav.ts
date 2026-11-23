import {
  ChartColumn,
  House,
  List,
  Trophy,
  type LucideIcon,
} from "lucide-react";

export type NavItem = { to: string; label: string; icon: LucideIcon };

/** Main navigation, in the order the sidebar and the phone menu show it. */
export const navItems: NavItem[] = [
  { to: "/", label: "Home", icon: House },
  { to: "/plan", label: "Plan", icon: List },
  { to: "/money", label: "Money", icon: ChartColumn },
  { to: "/ranks", label: "Ranks", icon: Trophy },
];

/** Shown at the bottom of the sidebar and phone menu until account sync exists (Milestone 2). */
export const healthLine = "No accounts linked yet";
