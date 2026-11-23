import { Bell, Settings } from "lucide-react";
import { Link } from "react-router";

/** Bell (with unread badge) and gear, shared by the desktop top bar and the phone bar. */
export function HeaderIcons({ unreadCount }: { unreadCount: number }) {
  const bellLabel =
    unreadCount > 0 ? `Notifications, ${unreadCount} unread` : "Notifications";
  return (
    <>
      <Link
        to="/notifications"
        aria-label={bellLabel}
        className="relative grid size-10 place-items-center rounded-full hover:bg-black/5"
      >
        <Bell className="size-5" aria-hidden />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-debt px-1 text-[10px] font-semibold text-white">
            {unreadCount}
          </span>
        )}
      </Link>
      <Link
        to="/settings"
        aria-label="Settings"
        className="grid size-10 place-items-center rounded-full hover:bg-black/5"
      >
        <Settings className="size-5" aria-hidden />
      </Link>
    </>
  );
}
