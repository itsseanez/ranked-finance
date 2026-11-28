import { Ellipsis } from "lucide-react";
import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { text } from "@/features/accounts/text";

export type MenuAction =
  | "rename"
  | "editDetails"
  | "hide"
  | "unhide"
  | "markClosed"
  | "reopen"
  | "delete";

type AccountMenuProps = {
  accountName: string;
  actions: MenuAction[];
  onSelect: (action: MenuAction) => void;
};

/**
 * The ··· menu on an account row. Follows the ARIA menu-button pattern: Enter, Space, or the
 * arrow keys open it; arrows, Home, and End move; Escape closes it and returns focus to the button.
 */
export function AccountMenu({
  accountName,
  actions,
  onSelect,
}: AccountMenuProps) {
  const menuId = useId();
  const [open, setOpen] = useState(false);
  const [initialFocus, setInitialFocus] = useState<"first" | "last">("first");
  const button = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLDivElement>(null);
  const items = () => [
    ...(menu.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? []),
  ];

  useEffect(() => {
    if (!open) return;
    const all = items();
    (initialFocus === "last" ? all[all.length - 1] : all[0])?.focus();
    // A click anywhere outside the menu closes it.
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!menu.current?.contains(target) && !button.current?.contains(target))
        setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [open, initialFocus]);

  const close = () => {
    setOpen(false);
    button.current?.focus();
  };

  const onButtonKeyDown = (event: KeyboardEvent) => {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setInitialFocus(event.key === "ArrowUp" ? "last" : "first");
      setOpen(true);
    }
  };

  const onMenuKeyDown = (event: KeyboardEvent) => {
    const all = items();
    const index = all.indexOf(document.activeElement as HTMLElement);
    const focusAt = (i: number) => all[(i + all.length) % all.length]?.focus();
    switch (event.key) {
      case "ArrowDown":
        focusAt(index + 1);
        break;
      case "ArrowUp":
        focusAt(index - 1);
        break;
      case "Home":
        focusAt(0);
        break;
      case "End":
        focusAt(all.length - 1);
        break;
      case "Escape":
        close();
        break;
      case "Tab":
        setOpen(false);
        return;
      default:
        return;
    }
    event.preventDefault();
    event.stopPropagation();
  };

  return (
    <div className="relative">
      <button
        ref={button}
        type="button"
        aria-label={text.menu.open(accountName)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => {
          setInitialFocus("first");
          setOpen((o) => !o);
        }}
        onKeyDown={onButtonKeyDown}
        className="grid size-11 place-items-center rounded-xl border border-line bg-card text-accent hover:bg-accent-tint focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        <Ellipsis className="size-4" aria-hidden />
      </button>
      {open && (
        <div
          ref={menu}
          id={menuId}
          role="menu"
          tabIndex={-1}
          aria-label={text.menu.open(accountName)}
          onKeyDown={onMenuKeyDown}
          className="absolute top-full right-0 z-20 mt-1 min-w-44 rounded-xl border border-line bg-card py-1 shadow-lg"
        >
          {actions.map((action) => (
            <button
              key={action}
              type="button"
              role="menuitem"
              tabIndex={-1}
              onClick={() => {
                close();
                onSelect(action);
              }}
              className={`block w-full px-4 py-2.5 text-left text-[15px] hover:bg-black/5 focus:bg-accent-tint focus:outline-2 focus:-outline-offset-2 focus:outline-accent ${
                action === "delete" ? "text-debt" : ""
              }`}
            >
              {text.menu[action]}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
