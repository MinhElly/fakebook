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
  await api.post(`/services/userservice/api/friend-requests/user/${targetUserId}`);
  return true;
}

export async function cancelFriendRequest(targetUserId: string, requestId?: string): Promise<boolean> {
  if (requestId) {
    await api.delete(`/services/userservice/api/friend-requests/${requestId}/cancel`);
    return true;
  }
  try {
    const res = await api.get<any[]>("/services/userservice/api/friend-requests/sent", {
      params: { page: 0, size: 100 },
    });
    const sentList = res.data || [];
    const found = sentList.find((req: any) => req.receiver?.id === targetUserId);
    if (found && found.id) {
      await api.delete(`/services/userservice/api/friend-requests/${found.id}/cancel`);
      return true;
    }
  } catch (err) {
    console.error("Lỗi khi tìm và hủy lời mời kết bạn:", err);
  }
  return false;
}

