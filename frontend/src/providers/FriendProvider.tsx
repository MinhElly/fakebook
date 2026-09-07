import { useState } from "react";
import { FriendContext } from "@/stores/friendStore";
import type { FriendUser, FriendStatus } from "@/types";

export default function FriendProvider({ children }: { children: React.ReactNode }) {
  const [friends] = useState<FriendUser[]>([]);
  const [following] = useState<FriendUser[]>([]);
  const [pendingReceived] = useState<FriendUser[]>([]);
  const [pendingSent] = useState<number[]>([]);

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
    void user;
  }

  function cancelRequest(userId: number) {
    void userId;
  }

  function acceptRequest(userId: number) {
    void userId;
  }

  function rejectRequest(userId: number) {
    void userId;
  }

  function removeFriend(userId: number) {
    void userId;
  }

  function follow(user: FriendUser) {
    void user;
  }

  function unfollow(userId: number) {
    void userId;
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
