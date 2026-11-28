import { useAccountHealth } from "@/features/accounts/api";
import { text } from "@/features/accounts/text";

/**
 * "{n} up to date · {m} need attention" (or "No accounts yet") for the sidebar and phone menu.
 * The dot turns the warning color when something needs attention; the words say it too.
 */
export function HealthLine({
  className,
  dotClassName = "",
}: {
  className: string;
  dotClassName?: string;
}) {
  const health = useAccountHealth();
  if (!health.data) return null;
  const upToDate = health.data.upToDate ?? 0;
  const needsAttention = health.data.needsAttention ?? 0;
  const line =
    upToDate === 0 && needsAttention === 0
      ? text.health.none
      : text.health.upToDate(upToDate) +
        text.health.separator +
        text.health.needAttention(needsAttention);
  return (
    <p className={className}>
      <span
        className={`${dotClassName} size-2 shrink-0 rounded-full ${
          needsAttention > 0 ? "bg-warn" : "bg-ink/30"
        }`}
        aria-hidden
      />
      {line}
    </p>
  );
}
