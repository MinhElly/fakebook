import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useAuth } from "@/providers/AuthProvider";
import { PresenceConnection, UNKNOWN_PRESENCE, type Presence } from "@/services/presenceService";

interface Context {
  entries: Record<string, Presence>;
  connected: boolean;
  watch: (ids: string[]) => () => void;
}
const PresenceContext = createContext<Context>({ entries: {}, connected: false, watch: () => () => {} });

export default function PresenceProvider({ children }: { children: ReactNode }) {
  const { status, user } = useAuth();
  const [entries, setEntries] = useState<Record<string, Presence>>({});
  const [connected, setConnected] = useState(false);
  const connection = useRef<PresenceConnection | null>(null);
  const watchers = useRef(new Map<symbol, string[]>());

  const syncTargets = useCallback(() => {
    connection.current?.setTargets([...new Set([...watchers.current.values()].flat())]);
  }, []);
  const watch = useCallback((ids: string[]) => {
    const key = Symbol(); watchers.current.set(key, ids); syncTargets();
    return () => { watchers.current.delete(key); syncTargets(); };
  }, [syncTargets]);

  useEffect(() => {
    setEntries({}); setConnected(false);
    if (status !== "authenticated" || !user?.id) return;
    let mounted = true;
    const socket = new PresenceConnection((updates, reset) => {
      if (mounted) setEntries(previous => reset ? updates : { ...previous, ...updates });
    }, value => { if (mounted) setConnected(value); });
    connection.current = socket; syncTargets(); socket.start();
    return () => { mounted = false; connection.current = null; socket.stop(); };
  }, [status, user?.id, syncTargets]);

  const value = useMemo(() => ({ entries, connected, watch }), [entries, connected, watch]);
  return <PresenceContext.Provider value={value}>{children}</PresenceContext.Provider>;
}

export function usePresences(ids: string[]) {
  const { entries, connected, watch } = useContext(PresenceContext);
  const key = [...new Set(ids.filter(Boolean))].sort().join(",");
  useEffect(() => watch(key ? key.split(",") : []), [key, watch]);
  return { entries, connected };
}

export function usePresence(id?: string, enabled = true): Presence {
  const { entries } = usePresences(id && enabled ? [id] : []);
  return id && enabled ? entries[id] || UNKNOWN_PRESENCE : UNKNOWN_PRESENCE;
}
