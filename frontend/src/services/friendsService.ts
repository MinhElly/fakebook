import api from "./apis";

export interface UserSummary {
  id: string;
  username?: string;
  displayName?: string;
  avatarMediaId?: string;
  avatarUrl?: string;
  coverMediaId?: string;
  bio?: string;
  location?: string;
  education?: string;
  work?: string;
}

export function getUserAvatarUrl(user?: UserSummary | null): string {
  if (!user) return "/default-avatar.svg";
  if (user.avatarUrl && (user.avatarUrl.startsWith("http://") || user.avatarUrl.startsWith("https://") || user.avatarUrl.startsWith("data:"))) {
    return user.avatarUrl;
  }
  if (user.avatarMediaId) {
    return `/services/mediaservice/api/media/${user.avatarMediaId}`;
  }
  return "/default-avatar.svg";
}

export interface FriendRequestItem {
  id: string;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";
  createdAt: string;
  respondedAt?: string;
  sender: UserSummary;
  receiver: UserSummary;
}

export interface FriendshipItem {
  id: string;
  createdAt: string;
  user: UserSummary;
  friend: UserSummary;
}

export interface FriendSuggestionItem {
  id: string;
  username: string;
  displayName: string;
  avatarMediaId?: string;
  coverMediaId?: string;
  bio?: string;
  location?: string;
  mutualFriendsCount?: number;
}

export type FriendTabType = "overview" | "requests" | "suggestions" | "all";

// Lấy thông tin user profile của người dùng hiện tại từ Keycloak Token
export async function getCurrentUserProfile(): Promise<UserSummary> {
  const response = await api.get<UserSummary>("/services/userservice/api/user-profiles/me");
  return response.data;
}

// Lấy danh sách lời mời kết bạn đã nhận (PENDING)
export async function getFriendRequests(myUserId: string): Promise<FriendRequestItem[]> {
  const response = await api.get<FriendRequestItem[]>("/services/userservice/api/friend-requests", {
    params: {
      "receiverId.equals": myUserId,
      "status.equals": "PENDING",
      sort: "createdAt,desc",
    },
  });
  return response.data || [];
}

// Lấy danh sách lời mời kết bạn đã gửi đi (PENDING)
export async function getSentFriendRequests(myUserId: string): Promise<FriendRequestItem[]> {
  const response = await api.get<FriendRequestItem[]>("/services/userservice/api/friend-requests", {
    params: {
      "senderId.equals": myUserId,
      "status.equals": "PENDING",
      sort: "createdAt,desc",
    },
  });
  return response.data || [];
}

// Phê duyệt lời mời kết bạn (Chấp nhận)
export async function acceptFriendRequest(request: FriendRequestItem): Promise<boolean> {
  try {
    // 1. Cập nhật trạng thái FriendRequest thành ACCEPTED
    await api.patch(`/services/userservice/api/friend-requests/${request.id}`, {
      id: request.id,
      status: "ACCEPTED",
      respondedAt: new Date().toISOString(),
    });

    // 2. Tạo bản ghi Friendship
    await api.post("/services/userservice/api/friendships", {
      createdAt: new Date().toISOString(),
      user: { id: request.receiver.id },
      friend: { id: request.sender.id },
    });

    return true;
  } catch (error) {
    console.error("Lỗi khi chấp nhận lời mời kết bạn:", error);
    throw error;
  }
}

// Từ chối lời mời kết bạn
export async function rejectFriendRequest(requestId: string): Promise<boolean> {
  try {
    await api.delete(`/services/userservice/api/friend-requests/${requestId}`);
    return true;
  } catch (error) {
    console.error("Lỗi khi xóa/từ chối lời mời kết bạn:", error);
    throw error;
  }
}

// Gửi lời mời kết bạn mới
export async function sendFriendRequest(myUserId: string, targetUserId: string): Promise<FriendRequestItem> {
  const response = await api.post<FriendRequestItem>("/services/userservice/api/friend-requests", {
    status: "PENDING",
    createdAt: new Date().toISOString(),
    sender: { id: myUserId },
    receiver: { id: targetUserId },
  });
  return response.data;
}

// Hủy lời mời kết bạn đã gửi
export async function cancelFriendRequest(requestId: string): Promise<boolean> {
  try {
    await api.delete(`/services/userservice/api/friend-requests/${requestId}`);
    return true;
  } catch (error) {
    console.error("Lỗi khi hủy lời mời kết bạn đã gửi:", error);
    throw error;
  }
}

// Lấy danh sách tất cả bạn bè của user hiện tại
export async function getAllFriends(myUserId: string): Promise<FriendshipItem[]> {
  const response = await api.get<FriendshipItem[]>("/services/userservice/api/friendships", {
    params: {
      "userId.equals": myUserId,
      sort: "createdAt,desc",
    },
  });
  return response.data || [];
}

// Hủy kết bạn
export async function unfriend(friendshipId: string): Promise<boolean> {
  try {
    await api.delete(`/services/userservice/api/friendships/${friendshipId}`);
    return true;
  } catch (error) {
    console.error("Lỗi khi hủy kết bạn:", error);
    throw error;
  }
}

// Lấy danh sách gợi ý bạn bè công khai
export async function getFriendSuggestions(myUserId: string): Promise<FriendSuggestionItem[]> {
  const response = await api.get<FriendSuggestionItem[]>("/services/userservice/api/user-profiles/public", {
    params: {
      page: 0,
      size: 20,
    },
  });
  const allUsers = response.data || [];
  // Lọc bỏ tài khoản cá nhân của chính mình
  return allUsers.filter((user) => user.id !== myUserId);
}
