import { ChevronLeft, Plus } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router";
import { AccountList } from "@/features/accounts/AccountList";
import { AddManualAccountDialog } from "@/features/accounts/AddManualAccountDialog";
import { text } from "@/features/accounts/text";

/**
 * Settings & accounts. M2a builds the header and the account column; the right-hand settings
 * sections (appearance, about you, income, notifications) arrive with features/settings in M7.
 */
export function SettingsAccountsPage() {
  const [adding, setAdding] = useState(false);

  return (
    <div className="mx-auto max-w-[1160px]">
      {/* Phones (mockup 23) get a way back home above the title. */}
      <Link
        to="/"
        className="mb-4 inline-flex items-center gap-2 rounded-lg text-[17px] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent min-[900px]:hidden"
      >
        <ChevronLeft className="size-5" aria-hidden />
        {text.page.back}
      </Link>
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[28px] leading-tight font-bold min-[900px]:font-semibold">
            <span className="min-[900px]:hidden">{text.page.phoneTitle}</span>
            <span className="max-[899px]:hidden">{text.page.title}</span>
          </h1>
          <p className="mt-1 text-[15px] text-ink/70 max-[899px]:hidden">
            {text.page.subtitle}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setAdding(true)}
          className="inline-flex h-12 shrink-0 items-center gap-2 rounded-xl bg-accent px-5 text-[15px] font-semibold text-white hover:bg-accent/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          <Plus className="size-4" aria-hidden />
          <span className="min-[900px]:hidden">{text.page.phoneConnect}</span>
          <span className="max-[899px]:hidden">{text.page.connect}</span>
        </button>
      </header>
      <div className="mt-6 grid gap-6 min-[1200px]:grid-cols-[minmax(0,1fr)_436px]">
        <AccountList />
      </div>
      {adding && <AddManualAccountDialog onClose={() => setAdding(false)} />}
    </div>
  );
}
