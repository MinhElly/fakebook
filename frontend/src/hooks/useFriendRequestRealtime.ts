import { useEffect, useRef, useState, useCallback } from "react";
import {
  getCurrentUserProfile,
  getFriendRequests,
  acceptFriendRequest,
  rejectFriendRequest,
  type FriendRequestItem,
} from "@/services/friendsService";

export function useFriendRequestRealtime(enabled: boolean, pollIntervalMs = 5000) {
  const [activeToastRequest, setActiveToastRequest] = useState<FriendRequestItem | null>(null);
  const seenRequestIds = useRef<Set<string>>(new Set());
  const isFirstFetch = useRef<boolean>(true);
  const myUserIdRef = useRef<string | null>(null);

  const checkIncomingRequests = useCallback(async () => {
    try {
      if (!myUserIdRef.current) {
        const user = await getCurrentUserProfile();
        if (user && user.id) {
          myUserIdRef.current = user.id;
        } else {
          return;
        }
      }

      const requests = await getFriendRequests(myUserIdRef.current);

      if (isFirstFetch.current) {
        // Lần đầu tải: Đánh dấu tất cả request hiện tại là đã thấy để tránh bật toast dồn dập
        requests.forEach((r) => seenRequestIds.current.add(r.id));
        isFirstFetch.current = false;
        return;
      }

      // Tìm lời mời mới xuất hiện chưa nằm trong seenRequestIds
      const newRequest = requests.find((r) => !seenRequestIds.current.has(r.id));
      if (newRequest) {
        seenRequestIds.current.add(newRequest.id);
        setActiveToastRequest(newRequest);
      }
    } catch (err) {
      console.warn("Polling friend requests background error:", err);
    }
  }, []);

  useEffect(() => {
    if (!enabled) {
      myUserIdRef.current = null;
      seenRequestIds.current.clear();
      isFirstFetch.current = true;
      setActiveToastRequest(null);
      return;
    }

    checkIncomingRequests();
    const interval = setInterval(checkIncomingRequests, pollIntervalMs);
    return () => clearInterval(interval);
  }, [enabled, checkIncomingRequests, pollIntervalMs]);

  const handleAcceptToast = async (req: FriendRequestItem) => {
    try {
      await acceptFriendRequest(req);
      setActiveToastRequest(null);
    } catch (err) {
      console.error("Lỗi khi chấp nhận lời mời từ toast:", err);
    }
  };

  const handleRejectToast = async (req: FriendRequestItem) => {
    try {
      await rejectFriendRequest(req.id);
      setActiveToastRequest(null);
    } catch (err) {
      console.error("Lỗi khi xóa lời mời từ toast:", err);
    }
  };

  const handleCloseToast = () => {
    setActiveToastRequest(null);
  };

  return {
    activeToastRequest,
    handleAcceptToast,
    handleRejectToast,
    handleCloseToast,
  };
}
