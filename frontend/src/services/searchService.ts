import api from "./apis";

export type FriendshipStatus = "NONE" | "PENDING_SENT" | "PENDING_RECEIVED" | "FRIEND";

export interface UserSearchResult {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  avatarMediaId?: string;
  coverUrl?: string;
  bio?: string;
  education?: string;
  workplace?: string;
  location?: string;
  mutualFriendsCount: number;
  friendshipStatus: FriendshipStatus;
}

export async function searchUsers(query: string, page = 0, size = 20): Promise<{ content: UserSearchResult[]; totalElements: number }> {
  if (!query || query.trim() === "") {
    return { content: [], totalElements: 0 };
  }

  const response = await api.get("/services/userservice/api/user-profiles/search", {
    params: { query: query.trim(), page, size },
  });

  return {
    content: response.data.content || response.data || [],
    totalElements: response.data.totalElements ?? response.data.length ?? 0,
  };
}

export async function sendFriendRequest(targetUserId: string): Promise<boolean> {
  await api.post("/services/userservice/api/friend-requests", {
    receiver: { id: targetUserId },
    status: "PENDING",
    createdAt: new Date().toISOString(),
  });
  return true;
}

export async function cancelFriendRequest(targetUserId: string): Promise<boolean> {
  await api.delete(`/services/userservice/api/friend-requests/target/${targetUserId}`);
  return true;
}
