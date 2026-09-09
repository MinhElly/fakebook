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

// In-memory cache for user profile summaries to avoid redundant API calls
const userProfileCache = new Map<string, UserSummary>();

export async function fetchUserProfileSummary(userId: string): Promise<UserSummary> {
  if (!userId) return { id: "" };
  if (userProfileCache.has(userId)) {
    return userProfileCache.get(userId)!;
  }
  try {
    const res = await api.get<UserSummary>(`/services/userservice/api/user-profiles/public/${userId}`);
    if (res.data) {
      userProfileCache.set(userId, res.data);
      return res.data;
    }
  } catch (err) {
    console.warn("Could not fetch user profile summary for id:", userId, err);
  }
  return { id: userId };
}

// Lấy thông tin user profile của người dùng hiện tại từ Keycloak Token
export async function getCurrentUserProfile(): Promise<UserSummary> {
  const response = await api.get<UserSummary>("/services/userservice/api/user-profiles/me");
  if (response.data && response.data.id) {
    userProfileCache.set(response.data.id, response.data);
  }
  return response.data;
}

// Lấy danh sách lời mời kết bạn đã nhận (PENDING) kèm bổ sung profile thông tin người gửi nếu bị thiếu
export async function getFriendRequests(myUserId?: string): Promise<FriendRequestItem[]> {
  const response = await api.get<FriendRequestItem[]>("/services/userservice/api/friend-requests/received", {
    params: {
      page: 0,
      size: 50,
    },
  });
  const rawList = response.data || [];

  return Promise.all(
    rawList.map(async (item) => {
      let sender = item.sender;
      let receiver = item.receiver;

      if (sender && (!sender.displayName || !sender.username)) {
        const enriched = await fetchUserProfileSummary(sender.id);
        sender = { ...sender, ...enriched };
      }
      if (receiver && (!receiver.displayName || !receiver.username)) {
        const enriched = await fetchUserProfileSummary(receiver.id);
        receiver = { ...receiver, ...enriched };
      }

      return { ...item, sender, receiver };
    })
  );
}

// Lấy danh sách lời mời kết bạn đã gửi đi (PENDING)
export async function getSentFriendRequests(myUserId?: string): Promise<FriendRequestItem[]> {
  const response = await api.get<FriendRequestItem[]>("/services/userservice/api/friend-requests/sent", {
    params: {
      page: 0,
      size: 50,
    },
  });
  const rawList = response.data || [];

  return Promise.all(
    rawList.map(async (item) => {
      let sender = item.sender;
      let receiver = item.receiver;

      if (sender && (!sender.displayName || !sender.username)) {
        const enriched = await fetchUserProfileSummary(sender.id);
        sender = { ...sender, ...enriched };
      }
      if (receiver && (!receiver.displayName || !receiver.username)) {
        const enriched = await fetchUserProfileSummary(receiver.id);
        receiver = { ...receiver, ...enriched };
      }

      return { ...item, sender, receiver };
    })
  );
}

// Phê duyệt lời mời kết bạn (Chấp nhận) - Backend tự động tạo bản ghi hai chiều an toàn
export async function acceptFriendRequest(request: FriendRequestItem): Promise<boolean> {
  try {
    await api.post(`/services/userservice/api/friend-requests/${request.id}/accept`);
    return true;
  } catch (error) {
    console.error("Lỗi khi chấp nhận lời mời kết bạn:", error);
    throw error;
  }
}

// Phê duyệt lời mời kết bạn theo ID lời mời
export async function acceptFriendRequestById(requestId: string, myUserId?: string, senderId?: string): Promise<boolean> {
  try {
    await api.post(`/services/userservice/api/friend-requests/${requestId}/accept`);
    return true;
  } catch (error) {
    console.error("Lỗi khi chấp nhận lời mời kết bạn:", error);
    throw error;
  }
}

// Từ chối lời mời kết bạn
export async function rejectFriendRequest(requestId: string): Promise<boolean> {
  try {
    await api.post(`/services/userservice/api/friend-requests/${requestId}/reject`);
    return true;
  } catch (error) {
    console.error("Lỗi khi từ chối lời mời kết bạn:", error);
    throw error;
  }
}

// Gửi lời mời kết bạn mới (Gọi API an toàn POST /api/friend-requests/user/{targetUserId})
export async function sendFriendRequest(arg1: string, arg2?: string): Promise<FriendRequestItem> {
  const targetUserId = arg2 || arg1;
  const response = await api.post<FriendRequestItem>(`/services/userservice/api/friend-requests/user/${targetUserId}`);

  let item = response.data;
  if (item && item.receiver && (!item.receiver.displayName || !item.receiver.username)) {
    const enriched = await fetchUserProfileSummary(targetUserId);
    item = { ...item, receiver: { ...item.receiver, ...enriched } };
  }
  return item;
}

// Hủy lời mời kết bạn đã gửi (Hỗ trợ linh hoạt cả requestId lẫn targetUserId)
export async function cancelFriendRequest(requestIdOrTargetUserId: string): Promise<boolean> {
  try {
    await api.delete(`/services/userservice/api/friend-requests/${requestIdOrTargetUserId}/cancel`);
    return true;
  } catch (error) {
    try {
      const res = await api.get<FriendRequestItem[]>("/services/userservice/api/friend-requests/sent", {
        params: { page: 0, size: 100 },
      });
      const sentList = res.data || [];
      const found = sentList.find(
        (req) => req.id === requestIdOrTargetUserId || req.receiver?.id === requestIdOrTargetUserId
      );
      if (found && found.id) {
        await api.delete(`/services/userservice/api/friend-requests/${found.id}/cancel`);
        return true;
      }
    } catch (fallbackErr) {
      console.error("Lỗi khi tìm lời mời kết bạn để hủy:", fallbackErr);
    }
    console.error("Lỗi khi hủy lời mời kết bạn đã gửi:", error);
    throw error;
  }
}

// Lấy danh sách tất cả bạn bè của user hiện tại (hỗ trợ truy vấn 2 chiều và tự động bổ sung profile)
export async function getAllFriends(myUserId: string): Promise<FriendshipItem[]> {
  const [res1, res2] = await Promise.all([
    api.get<FriendshipItem[]>("/services/userservice/api/friendships", {
      params: { "userId.equals": myUserId, sort: "createdAt,desc" },
    }).catch(() => ({ data: [] })),
    api.get<FriendshipItem[]>("/services/userservice/api/friendships", {
      params: { "friendId.equals": myUserId, sort: "createdAt,desc" },
    }).catch(() => ({ data: [] })),
  ]);

  const rawList1 = res1.data || [];
  const rawList2 = res2.data || [];
  const combined = [...rawList1, ...rawList2];

  // Khử trùng lặp theo ID bạn bè đối phương
  const friendMap = new Map<string, FriendshipItem>();
  for (const item of combined) {
    const targetFriendUser = item.friend?.id === myUserId ? item.user : item.friend;
    const targetId = targetFriendUser?.id;
    if (targetId && !friendMap.has(targetId)) {
      friendMap.set(targetId, item);
    }
  }

  const list = Array.from(friendMap.values());

  // Bổ sung profile thông tin hiển thị nếu bị thiếu từ MapStruct backend
  return Promise.all(
    list.map(async (item) => {
      let user = item.user;
      let friend = item.friend;
      if (user && (!user.displayName || !user.username)) {
        const enriched = await fetchUserProfileSummary(user.id);
        user = { ...user, ...enriched };
      }
      if (friend && (!friend.displayName || !friend.username)) {
        const enriched = await fetchUserProfileSummary(friend.id);
        friend = { ...friend, ...enriched };
      }
      return { ...item, user, friend };
    })
  );
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
