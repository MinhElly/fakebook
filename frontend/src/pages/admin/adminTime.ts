export const ADMIN_TIME_ZONE = "Asia/Ho_Chi_Minh";
export const ADMIN_TIME_NOTE = "Thời gian theo giờ Việt Nam (GMT+7)";

const formatter = new Intl.DateTimeFormat("en-GB", {
  timeZone: ADMIN_TIME_ZONE, day: "2-digit", month: "2-digit", year: "numeric",
  hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23",
});

function parts(value: unknown): Record<string, string> | null {
  if (value == null || value === "") return null;
  const instant = new Date(typeof value === "number" ? value : String(value));
  return Number.isNaN(instant.getTime()) ? null : Object.fromEntries(formatter.formatToParts(instant).map(part => [part.type, part.value]));
}

export function formatAdminDate(value: unknown): string {
  const p = parts(value);
  return p ? `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}:${p.second}` : "—";
}

export function adminFilterDate(value: string | null): string {
  const p = parts(value);
  return p ? `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}` : "";
}

export function adminFilterInstant(value: string): string {
  // datetime-local contains wall-clock time, explicitly interpreted as Vietnam time.
  return new Date(`${value}+07:00`).toISOString();
}
