import { MessageCircle } from "lucide-react";
import { HeaderIcons } from "@/app/shell/HeaderIcons";

type TopBarProps = {
  unreadCount: number;
  askOpen: boolean;
  onAskToggle: () => void;
};

/** Desktop top-right actions: Ask, bell, gear. */
export function TopBar({ unreadCount, askOpen, onAskToggle }: TopBarProps) {
  return (
    <header className="flex items-center justify-end gap-3 px-8 pt-[18px] pb-4">
      <button
        type="button"
        aria-expanded={askOpen}
        aria-controls="ask-panel"
        onClick={onAskToggle}
        className="flex h-10 items-center gap-2 rounded-full border border-accent px-4 text-sm font-medium text-accent hover:bg-accent-tint"
      >
        <MessageCircle className="size-4" aria-hidden />
        Ask
      </button>
      <HeaderIcons unreadCount={unreadCount} />
    </header>
  );
}
