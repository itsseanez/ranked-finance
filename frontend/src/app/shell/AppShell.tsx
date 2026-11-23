import { useState } from "react";
import { Outlet } from "react-router";
import { PhoneBar } from "@/app/shell/PhoneBar";
import { Sidebar } from "@/app/shell/Sidebar";
import { TopBar } from "@/app/shell/TopBar";
import { useMediaQuery } from "@/lib/use-media-query";

/** Layout around every signed-in page: sidebar + top bar on desktop, a top bar with a menu on phones. */
export function AppShell() {
  const isDesktop = useMediaQuery("(min-width: 900px)");
  const [askOpen, setAskOpen] = useState(false);
  const unreadCount = 0; // Notifications arrive in a later milestone.

  return (
    <div className={isDesktop ? "flex min-h-screen" : "min-h-screen"}>
      {isDesktop ? (
        <Sidebar />
      ) : (
        <PhoneBar unreadCount={unreadCount} onAsk={() => setAskOpen(true)} />
      )}
      <div className="flex min-w-0 flex-1 flex-col">
        {isDesktop && (
          <TopBar
            unreadCount={unreadCount}
            askOpen={askOpen}
            onAskToggle={() => setAskOpen((open) => !open)}
          />
        )}
        <main className={isDesktop ? "px-8 pb-8" : "p-4"}>
          <Outlet />
        </main>
      </div>
      {askOpen && (
        <aside
          id="ask-panel"
          aria-label="Ask"
          className="fixed inset-y-0 right-0 z-20 w-full max-w-[400px] border-l border-line bg-card p-6 shadow-xl"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Ask</h2>
            <button
              type="button"
              onClick={() => setAskOpen(false)}
              className="text-sm text-accent"
            >
              Close
            </button>
          </div>
        </aside>
      )}
    </div>
  );
}
