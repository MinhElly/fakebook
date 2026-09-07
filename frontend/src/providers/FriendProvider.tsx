import { useState } from "react";
import { FriendContext } from "@/stores/friendStore";
import {
  FRIEND_USERS,
  INITIAL_FRIENDS,
  INITIAL_FOLLOWING,
  INITIAL_PENDING_RECEIVED,
  INITIAL_PENDING_SENT,
} from "@/constants/data";
import type { FriendUser, FriendStatus } from "@/types";

export default function FriendProvider({ children }: { children: React.ReactNode }) {
  const [friends, setFriends] = useState<FriendUser[]>(
    FRIEND_USERS.filter(u => INITIAL_FRIENDS.includes(u.id))
  );
  const [following, setFollowing] = useState<FriendUser[]>(
    FRIEND_USERS.filter(u => INITIAL_FOLLOWING.includes(u.id))
  );
  const [pendingReceived, setPendingReceived] = useState<FriendUser[]>(
    FRIEND_USERS.filter(u => INITIAL_PENDING_RECEIVED.includes(u.id))
  );
  const [pendingSent, setPendingSent] = useState<number[]>(INITIAL_PENDING_SENT);

  function getStatus(userId: number): FriendStatus {
    if (friends.some(f => f.id === userId)) return "friends";
    if (pendingSent.includes(userId)) return "pending_sent";
    if (pendingReceived.some(f => f.id === userId)) return "pending_received";
    return "none";
  }

  function isFollowing(userId: number) {
    return following.some(f => f.id === userId);
  }

  function sendRequest(user: FriendUser) {
    setPendingSent(prev => [...prev, user.id]);
  }

  function cancelRequest(userId: number) {
    setPendingSent(prev => prev.filter(id => id !== userId));
  }

  function acceptRequest(userId: number) {
    const user = pendingReceived.find(u => u.id === userId);
    if (!user) return;
    setFriends(prev => [...prev, user]);
    setFollowing(prev => prev.some(f => f.id === userId) ? prev : [...prev, user]);
    setPendingReceived(prev => prev.filter(u => u.id !== userId));
  }

  function rejectRequest(userId: number) {
    setPendingReceived(prev => prev.filter(u => u.id !== userId));
  }

  function removeFriend(userId: number) {
    setFriends(prev => prev.filter(u => u.id !== userId));
  }

  function follow(user: FriendUser) {
    if (!following.some(f => f.id === user.id)) {
      setFollowing(prev => [...prev, user]);
    }
  }

  function unfollow(userId: number) {
    setFollowing(prev => prev.filter(u => u.id !== userId));
  }

  return (
    <FriendContext.Provider value={{
      friends, following, pendingReceived, pendingSent,
      getStatus, isFollowing, sendRequest, cancelRequest,
      acceptRequest, rejectRequest, removeFriend, follow, unfollow,
    }}>
      {children}
    </FriendContext.Provider>
  );
}
