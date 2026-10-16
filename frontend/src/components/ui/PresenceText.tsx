import { useEffect, useState } from "react";
import type { Presence } from "@/services/presenceService";

export function presenceLabel(presence: Presence, now = Date.now()) {
  if (presence.status === "online") return "Đang hoạt động";
  if (presence.status === "unknown") return "Chưa xác định trạng thái";
  if (!presence.lastSeenAt || !Number.isFinite(Date.parse(presence.lastSeenAt))) return "Ngoại tuyến";
  const minutes = Math.floor(Math.max(0, now - Date.parse(presence.lastSeenAt)) / 60000);
  if (minutes < 1) return "Hoạt động vừa xong";
  if (minutes < 60) return `Hoạt động ${minutes} phút trước`;
  if (minutes < 1440) return `Hoạt động ${Math.floor(minutes / 60)} giờ trước`;
  return `Hoạt động ${Math.floor(minutes / 1440)} ngày trước`;
}

export default function PresenceText({ presence, hideUnknown = false }: { presence: Presence; hideUnknown?: boolean }) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(timer);
  }, []);
  if (hideUnknown && presence.status === "unknown") return null;
  return <span className="block text-xs font-normal text-[#65676B]">{presenceLabel(presence, Math.max(now, Date.now()))}</span>;
}
