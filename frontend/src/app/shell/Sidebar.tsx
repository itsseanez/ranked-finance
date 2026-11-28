import { NavLink } from "react-router";
import { navItems } from "@/app/shell/nav";
import { HealthLine } from "@/features/accounts/HealthLine";

/** Desktop navigation (≥900px): sticky, full height, 220px wide. */
export function Sidebar() {
  return (
    <aside className="sticky top-0 flex h-screen w-[220px] shrink-0 flex-col border-r border-line bg-card">
      <div className="px-7 pt-6 pb-5 text-[15px] font-semibold">
        Ranked Finance
      </div>
      <nav aria-label="Main" className="flex flex-col gap-1 px-4">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) =>
              `flex h-11 items-center gap-3 rounded-[10px] px-3 text-[15px] ${
                isActive
                  ? "bg-accent-tint font-semibold text-accent"
                  : "hover:bg-black/5"
              }`
            }
          >
            <Icon className="size-[18px]" aria-hidden />
            {label}
          </NavLink>
        ))}
      </nav>
      <HealthLine
        className="mt-auto flex items-start gap-2 px-7 pb-6 text-xs text-ink/70"
        dotClassName="mt-1"
      />
    </aside>
  );
}
