import { createContext, useContext } from "react";
import type { FriendUser, FriendStatus } from "@/types";

export interface FriendState {
  friends: FriendUser[];
  following: FriendUser[];
  pendingReceived: FriendUser[];
  pendingSent: number[];
  getStatus: (userId: number) => FriendStatus;
  isFollowing: (userId: number) => boolean;
  sendRequest: (user: FriendUser) => void;
  cancelRequest: (userId: number) => void;
  acceptRequest: (userId: number) => void;
  rejectRequest: (userId: number) => void;
  removeFriend: (userId: number) => void;
  follow: (user: FriendUser) => void;
  unfollow: (userId: number) => void;
}

export const FriendContext = createContext<FriendState>({
  friends: [],
  following: [],
  pendingReceived: [],
  pendingSent: [],
  getStatus: () => "none",
  isFollowing: () => false,
  sendRequest: () => {},
  cancelRequest: () => {},
  acceptRequest: () => {},
  rejectRequest: () => {},
  removeFriend: () => {},
  follow: () => {},
  unfollow: () => {},
});

export function useFriendStore() {
  return useContext(FriendContext);
}
