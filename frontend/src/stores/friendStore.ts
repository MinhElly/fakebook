import { createContext, useContext } from "react";
import type { FriendUser, FriendStatus } from "@/types";

export interface FriendState {
  loading: boolean;
  loadError: string | null;
  retry: () => void;
  pendingUsers: Set<string>;
  friends: FriendUser[];
  following: FriendUser[];
  pendingReceived: FriendUser[];
  pendingSent: (string | number)[];
  getStatus: (userId: string | number) => FriendStatus;
  isFollowing: (userId: string | number) => boolean;
  sendRequest: (user: FriendUser) => void;
  cancelRequest: (userId: string | number) => void;
  acceptRequest: (userId: string | number) => void;
  rejectRequest: (userId: string | number) => void;
  removeFriend: (userId: string | number) => void;
  follow: (user: FriendUser) => void;
  unfollow: (userId: string | number) => void;
}

export const FriendContext = createContext<FriendState>({
  loading: true, loadError: null, retry: () => {}, pendingUsers: new Set(),
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
