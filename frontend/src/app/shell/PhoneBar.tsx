import { Menu, MessageCircle, X } from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink } from "react-router";
import { HeaderIcons } from "@/app/shell/HeaderIcons";
import { healthLine, navItems } from "@/app/shell/nav";

type PhoneBarProps = { unreadCount: number; onAsk: () => void };

/** Phone navigation (<900px): a top bar whose Menu button opens the nav list. Never bottom tabs. */
export function PhoneBar({ unreadCount, onAsk }: PhoneBarProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const close = () => setMenuOpen(false);

  // Escape closes the menu, as it does for any menu or dialog.
  useEffect(() => {
    if (!menuOpen) return;
    const onKey = (event: KeyboardEvent) =>
      event.key === "Escape" && setMenuOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [menuOpen]);
  const itemClass = "flex h-[52px] items-center gap-4 rounded-2xl px-4 text-lg";

  return (
    <div className="sticky top-0 z-10 bg-card">
      <header className="grid h-14 grid-cols-[auto_1fr_auto] items-center border-b border-line px-2">
        <button
          type="button"
          aria-label={menuOpen ? "Close menu" : "Menu"}
          aria-expanded={menuOpen}
          aria-controls="phone-menu"
          onClick={() => setMenuOpen((open) => !open)}
          className="grid size-10 place-items-center rounded-full hover:bg-black/5"
        >
          {menuOpen ? (
            <X className="size-5" aria-hidden />
          ) : (
            <Menu className="size-5" aria-hidden />
          )}
        </button>
        <span className="text-center text-[17px] font-semibold">
          Ranked Finance
        </span>
        <div className="flex items-center">
          <HeaderIcons unreadCount={unreadCount} />
        </div>
      </header>

      {menuOpen && (
        // Phones: the menu covers the page. Tablets (≥640px): a 320px drawer over a dimmed page; tap outside to close.
        <div className="fixed inset-x-0 top-14 bottom-0">
          <button
            type="button"
            aria-label="Close menu"
            tabIndex={-1}
            onClick={close}
            className="absolute inset-0 hidden bg-black/30 sm:block"
          />
          <nav
            id="phone-menu"
            aria-label="Main"
            className="relative h-full overflow-y-auto bg-card px-4 pt-4 sm:w-80 sm:border-r sm:border-line sm:shadow-xl"
          >
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === "/"}
                onClick={close}
                className={({ isActive }) =>
                  `${itemClass} ${isActive ? "bg-accent-tint font-semibold text-accent" : ""}`
                }
              >
                <Icon className="size-5" aria-hidden />
                {label}
              </NavLink>
            ))}
            <button
              type="button"
              onClick={() => {
                close();
                onAsk();
              }}
              className={`${itemClass} w-full`}
            >
              <MessageCircle className="size-5" aria-hidden />
              Ask
            </button>
            <p className="mt-3 flex items-center gap-2 border-t border-line px-4 pt-4 text-sm text-ink/70">
              <span className="size-2 rounded-full bg-ink/30" aria-hidden />
              {healthLine}
            </p>
          </nav>
        </div>
      )}
    </div>
  );
}
