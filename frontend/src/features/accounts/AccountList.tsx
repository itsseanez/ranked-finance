import { AccountCard } from "@/features/accounts/AccountCard";
import { groupAccounts, useAccounts } from "@/features/accounts/api";
import { FormError } from "@/features/accounts/Field";
import { text } from "@/features/accounts/text";
import { messageFor } from "@/lib/errors";

/** Every account, one card per institution, closed accounts last. */
export function AccountList() {
  const accounts = useAccounts();

  if (accounts.isPending) {
    return <p className="text-sm text-ink/70">{text.list.loading}</p>;
  }
  if (accounts.isError) {
    return <FormError message={messageFor(accounts.error)} />;
  }
  if (accounts.data.length === 0) {
    return (
      <p className="rounded-2xl border border-line bg-card p-5 text-[15px] text-ink/70">
        {text.list.empty}
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      {groupAccounts(accounts.data).map((group) => (
        <AccountCard key={group.key} group={group} />
      ))}
    </div>
  );
}
