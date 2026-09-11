import { useState, useEffect, useCallback } from "react";
import { FriendContext } from "@/stores/friendStore";
import type { FriendUser, FriendStatus } from "@/types";
import {
  getMyFriends,
  getFriendRequests,
  getSentFriendRequests,
  sendFriendRequest,
  cancelFriendRequest,
  acceptFriendRequestById,
  rejectFriendRequest,
  unfriend,
  getUserAvatarUrl,
} from "@/services/friendsService";
import {
  getMyFollowingList,
  followUser,
  unfollowUser,
} from "@/services/followService";

import { useAuth } from "@/providers/AuthProvider";

export default function FriendProvider({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const [friends, setFriends] = useState<FriendUser[]>([]);
  const [following, setFollowing] = useState<FriendUser[]>([]);
  const [pendingReceived, setPendingReceived] = useState<FriendUser[]>([]);
  const [pendingSent, setPendingSent] = useState<(string | number)[]>([]);

  // Tải dữ liệu bạn bè & follow ban đầu từ Backend
  const loadInitialData = useCallback(async () => {
    try {
      const [friendsRes, followingRes, receivedRes, sentRes] = await Promise.all([
        getMyFriends().catch(() => []),
        getMyFollowingList().catch(() => []),
        getFriendRequests().catch(() => []),
        getSentFriendRequests().catch(() => []),
      ]);

      // Map bạn bè
      const mappedFriends: FriendUser[] = friendsRes.map((item) => {
        const friendObj = item.friend || item.user;
        return {
          id: friendObj.id,
          name: friendObj.displayName || friendObj.username || "User",
          avatar: getUserAvatarUrl(friendObj),
          cover: friendObj.coverMediaId ? `/services/mediaservice/api/media/${friendObj.coverMediaId}` : "/default-cover.svg",
          mutualFriends: 0,
          location: friendObj.location || "",
          work: friendObj.work || "",
          education: friendObj.education || "",
          bio: friendObj.bio || "",
        };
      });
      setFriends(mappedFriends);

      // Map following
      const mappedFollowing: FriendUser[] = followingRes.map((item) => {
        const target = item.following || item.follower;
        return {
          id: target ? target.id : item.id || "",
          name: target ? (target.displayName || target.username || "User") : "User",
          avatar: getUserAvatarUrl(target),
          cover: target?.coverMediaId ? `/services/mediaservice/api/media/${target.coverMediaId}` : "/default-cover.svg",
          mutualFriends: 0,
          location: target?.location || "",
          work: target?.work || "",
          education: target?.education || "",
          bio: target?.bio || "",
        };
      });
      setFollowing(mappedFollowing);

      // Map pending received
      const mappedReceived: FriendUser[] = receivedRes.map((item) => {
        const sender = item.sender;
        return {
          id: sender.id,
          name: sender.displayName || sender.username || "User",
          avatar: getUserAvatarUrl(sender),
          cover: sender.coverMediaId ? `/services/mediaservice/api/media/${sender.coverMediaId}` : "/default-cover.svg",
          mutualFriends: 0,
          location: sender.location || "",
          work: sender.work || "",
          education: sender.education || "",
          bio: sender.bio || "",
        };
      });
      setPendingReceived(mappedReceived);

      // Map pending sent IDs
      const sentIds = sentRes.map((item) => item.receiver?.id || item.id);
      setPendingSent(sentIds);
    } catch (err) {
      console.error("Lỗi khi tải dữ liệu FriendProvider:", err);
    }
  }, []);

  useEffect(() => {
    if (status !== "authenticated") return;
    loadInitialData();
  }, [status, loadInitialData]);

  function getStatus(userId: string | number): FriendStatus {
    const idStr = String(userId);
    if (friends.some((f) => String(f.id) === idStr)) return "friends";
    if (pendingSent.some((id) => String(id) === idStr)) return "pending_sent";
    if (pendingReceived.some((f) => String(f.id) === idStr)) return "pending_received";
    return "none";
  }

  function isFollowing(userId: string | number): boolean {
    const idStr = String(userId);
    return following.some((f) => String(f.id) === idStr);
  }

  async function sendRequest(user: FriendUser) {
    try {
      const idStr = String(user.id);
      setPendingSent((prev) => [...prev, user.id]);
      await sendFriendRequest(idStr);
    } catch (err) {
      console.error("Lỗi khi gửi lời mời kết bạn:", err);
      setPendingSent((prev) => prev.filter((id) => String(id) !== String(user.id)));
    }
  }

  async function cancelRequest(userId: string | number) {
    try {
      const idStr = String(userId);
      setPendingSent((prev) => prev.filter((id) => String(id) !== idStr));
      await cancelFriendRequest(idStr);
    } catch (err) {
      console.error("Lỗi khi hủy lời mời kết bạn:", err);
      setPendingSent((prev) => [...prev, userId]);
    }
  }

  async function acceptRequest(userId: string | number) {
    try {
      const idStr = String(userId);
      const targetUser = pendingReceived.find((u) => String(u.id) === idStr);
      setPendingReceived((prev) => prev.filter((u) => String(u.id) !== idStr));
      if (targetUser) {
        setFriends((prev) => [...prev, targetUser]);
      }
      // Tìm requestId thích hợp để gọi API
      const receivedList = await getFriendRequests();
      const match = receivedList.find((req) => String(req.sender.id) === idStr);
      if (match) {
        await acceptFriendRequestById(match.id);
      }
    } catch (err) {
      console.error("Lỗi khi chấp nhận lời mời kết bạn:", err);
      loadInitialData();
    }
  }

  async function rejectRequest(userId: string | number) {
    try {
      const idStr = String(userId);
      setPendingReceived((prev) => prev.filter((u) => String(u.id) !== idStr));
      const receivedList = await getFriendRequests();
      const match = receivedList.find((req) => String(req.sender.id) === idStr);
      if (match) {
        await rejectFriendRequest(match.id);
      }
    } catch (err) {
      console.error("Lỗi khi từ chối lời mời kết bạn:", err);
      loadInitialData();
    }
  }

  async function removeFriend(userId: string | number) {
    try {
      const idStr = String(userId);
      setFriends((prev) => prev.filter((f) => String(f.id) !== idStr));
      await unfriend(idStr);
    } catch (err) {
      console.error("Lỗi khi hủy kết bạn:", err);
      loadInitialData();
    }
  }

  async function follow(user: FriendUser) {
    try {
      const idStr = String(user.id);
      setFollowing((prev) => [...prev, user]);
      await followUser(idStr);
    } catch (err) {
      console.error("Lỗi khi theo dõi người dùng:", err);
      setFollowing((prev) => prev.filter((f) => String(f.id) !== String(user.id)));
    }
  }

  async function unfollow(userId: string | number) {
    try {
      const idStr = String(userId);
      setFollowing((prev) => prev.filter((f) => String(f.id) !== idStr));
      await unfollowUser(idStr);
    } catch (err) {
      console.error("Lỗi khi bỏ theo dõi người dùng:", err);
      loadInitialData();
    }
  }

  return (
    <FriendContext.Provider
      value={{
        friends,
        following,
        pendingReceived,
        pendingSent,
        getStatus,
        isFollowing,
        sendRequest,
        cancelRequest,
        acceptRequest,
        rejectRequest,
        removeFriend,
        follow,
        unfollow,
      }}
    >
      {children}
    </FriendContext.Provider>
  );
}
