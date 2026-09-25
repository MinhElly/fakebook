import { useCallback, useEffect, useRef, useState } from "react";

import { useRealtime } from "@/providers/RealtimeProvider";
import {
  acceptFriendRequest,
  getCurrentUserProfile,
  getFriendRequests,
  rejectFriendRequest,
  type FriendRequestItem,
} from "@/services/friendsService";

export function useFriendRequestRealtime(enabled: boolean) {
  const { subscribe, subscribeConnection } = useRealtime();
  const [activeToastRequest, setActiveToastRequest] = useState<FriendRequestItem | null>(null);
  const seenRequestIds = useRef(new Set<string>());
  const isFirstFetch = useRef(true);
  const myUserIdRef = useRef<string | null>(null);
  const activeRef = useRef(false);
  const checkingRef = useRef(false);
  const checkQueuedRef = useRef(false);

  const checkIncomingRequests = useCallback(async () => {
    if (!activeRef.current) return;
    if (checkingRef.current) {
      checkQueuedRef.current = true;
      return;
    }

    checkingRef.current = true;
    try {
      do {
        checkQueuedRef.current = false;

        if (!myUserIdRef.current) {
          const user = await getCurrentUserProfile();
          if (!activeRef.current || !user?.id) return;
          myUserIdRef.current = user.id;
        }

        const requests = await getFriendRequests(myUserIdRef.current);
        if (!activeRef.current) return;

        if (isFirstFetch.current) {
          requests.forEach(request => seenRequestIds.current.add(request.id));
          isFirstFetch.current = false;
          continue;
        }

        const newRequest = requests.find(request => !seenRequestIds.current.has(request.id));
        if (newRequest) {
          seenRequestIds.current.add(newRequest.id);
          setActiveToastRequest(newRequest);
        }
      } while (checkQueuedRef.current && activeRef.current);
    } catch (error) {
      console.warn("Friend request realtime reconciliation failed", error);
    } finally {
      checkingRef.current = false;
    }
  }, []);

  useEffect(() => {
    activeRef.current = enabled;
    if (!enabled) {
      myUserIdRef.current = null;
      seenRequestIds.current.clear();
      isFirstFetch.current = true;
      checkQueuedRef.current = false;
      setActiveToastRequest(null);
      return;
    }

    void checkIncomingRequests();

    const unsubscribeEvents = subscribe(event => {
      if (event.eventType === "FRIEND_REQUEST_CREATED") {
        void checkIncomingRequests();
      }
    });
    const unsubscribeConnection = subscribeConnection(() => {
      void checkIncomingRequests();
    });

    return () => {
      activeRef.current = false;
      unsubscribeEvents();
      unsubscribeConnection();
    };
  }, [checkIncomingRequests, enabled, subscribe, subscribeConnection]);

  const handleAcceptToast = async (request: FriendRequestItem) => {
    try {
      await acceptFriendRequest(request);
      setActiveToastRequest(null);
    } catch (error) {
      console.error("Could not accept friend request", error);
    }
  };

  const handleRejectToast = async (request: FriendRequestItem) => {
    try {
      await rejectFriendRequest(request.id);
      setActiveToastRequest(null);
    } catch (error) {
      console.error("Could not reject friend request", error);
    }
  };

  return {
    activeToastRequest,
    handleAcceptToast,
    handleRejectToast,
    handleCloseToast: () => setActiveToastRequest(null),
  };
}
