import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
} from "react";

import { useAuth } from "@/providers/AuthProvider";
import { connectRealtime, type RealtimeEvent } from "@/services/realtimeService";

type RealtimeListener = (event: RealtimeEvent) => void;
type ConnectionListener = () => void;

interface RealtimeContextValue {
  subscribe: (listener: RealtimeListener) => () => void;
  subscribeConnection: (listener: ConnectionListener) => () => void;
}

const RealtimeContext = createContext<RealtimeContextValue | undefined>(undefined);

export default function RealtimeProvider({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const listenersRef = useRef(new Set<RealtimeListener>());
  const connectionListenersRef = useRef(new Set<ConnectionListener>());

  const subscribe = useCallback((listener: RealtimeListener) => {
    listenersRef.current.add(listener);
    return () => listenersRef.current.delete(listener);
  }, []);

  const subscribeConnection = useCallback((listener: ConnectionListener) => {
    connectionListenersRef.current.add(listener);
    return () => connectionListenersRef.current.delete(listener);
  }, []);

  useEffect(() => {
    if (status !== "authenticated") return;

    const controller = new AbortController();

    void connectRealtime(
      controller.signal,
      event => {
        listenersRef.current.forEach(listener => {
          try {
            listener(event);
          } catch (error) {
            console.error("[Realtime] Listener failed", error);
          }
        });
      },
      () => {
        connectionListenersRef.current.forEach(listener => {
          try {
            listener();
          } catch (error) {
            console.error("[Realtime] Connection listener failed", error);
          }
        });
      },
    ).catch(error => {
      if (!controller.signal.aborted) {
        console.error("[Realtime] Connection stopped", error);
      }
    });

    return () => controller.abort();
  }, [status]);

  const value = useMemo(
    () => ({ subscribe, subscribeConnection }),
    [subscribe, subscribeConnection],
  );

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}

export function useRealtime(): RealtimeContextValue {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error("useRealtime must be used inside RealtimeProvider");
  }
  return context;
}
