import { useEffect, useRef, useState } from "react";
import { usePresence } from "@/providers/PresenceProvider";
import PresenceBadge from "@/components/ui/PresenceBadge";
import PresenceText from "@/components/ui/PresenceText";

/** Subscribe cards near the viewport so long lists stay within the 100-user socket limit. */
export default function PresenceIndicator({ userId, enabled = true, hideUnknown = false }: {
  userId: string; enabled?: boolean; hideUnknown?: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);
  const presence = usePresence(userId, enabled && visible);
  useEffect(() => {
    if (!enabled || !ref.current) return;
    const observer = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting), { rootMargin: "150px" });
    observer.observe(ref.current);
    return () => observer.disconnect();
  }, [enabled, userId]);
  return <div ref={ref} className="relative min-h-4 pl-4">
    {enabled && <><span className="absolute left-0 top-1 h-3 w-3"><PresenceBadge status={presence.status} /></span><PresenceText presence={presence} hideUnknown={hideUnknown} /></>}
  </div>;
}
