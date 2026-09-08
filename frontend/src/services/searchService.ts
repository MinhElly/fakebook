import api from "./apis";

export type FriendshipStatus = "NONE" | "PENDING_SENT" | "PENDING_RECEIVED" | "FRIEND";

export interface UserSearchResult {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  coverUrl?: string;
  bio?: string;
  education?: string;
  workplace?: string;
  location?: string;
  mutualFriendsCount: number;
  friendshipStatus: FriendshipStatus;
}

// Fallback mock database for instant UI testing when backend API is not yet live
const MOCK_SEARCH_DATABASE: UserSearchResult[] = [
  {
    id: "user-huyen-1",
    username: "phamhuyen",
    displayName: "Phạm Huyền",
    avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
    coverUrl: "https://images.unsplash.com/photo-1707343843437-caacff5cfa74?w=600&auto=format&fit=crop&q=80",
    bio: "Sống là cống hiến ✨",
    education: "Đại học FPT Hà Nội",
    workplace: "Làm việc tại Chúc Đăng Điền",
    location: "Hà Nội",
    mutualFriendsCount: 81,
    friendshipStatus: "FRIEND",
  },
  {
    id: "user-huyen-2",
    username: "dghuyen.02",
    displayName: "Dương Thị Thu Huyền",
    avatarUrl: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
    bio: "Người sáng tạo nội dung số · 2,4K người theo dõi · @dghuyen.02",
    education: "Đại học FPT Hà Nội",
    workplace: "Content Creator",
    location: "Hà Nội",
    mutualFriendsCount: 19,
    friendshipStatus: "PENDING_SENT",
  },
  {
    id: "user-huyen-3",
    username: "khanhhuyen",
    displayName: "Khánh Huyền",
    avatarUrl: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150&auto=format&fit=crop&q=80",
    bio: "Trường Đại học FPT · Sống tại Vĩnh Yên",
    education: "Trường Đại học FPT",
    location: "Vĩnh Yên",
    mutualFriendsCount: 11,
    friendshipStatus: "NONE",
  },
  {
    id: "user-huyen-4",
    username: "huyenanhh",
    displayName: "Huyen Anhh",
    avatarUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
    bio: "Sống tại Hà Nội",
    location: "Hà Nội",
    mutualFriendsCount: 0,
    friendshipStatus: "NONE",
  },
  {
    id: "user-huyen-5",
    username: "nguyenhuyenlinh",
    displayName: "Nguyen Huyen Linh",
    avatarUrl: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
    bio: "THPT Nguyễn Viết Xuân · 358 người theo dõi",
    education: "THPT Nguyễn Viết Xuân",
    mutualFriendsCount: 6,
    friendshipStatus: "NONE",
  },
  {
    id: "user-minh-1",
    username: "minhnguyen",
    displayName: "Nguyễn Minh",
    avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
    bio: "Software Engineer @ Fakebook",
    education: "Đại học Bách Khoa Hà Nội",
    workplace: "Fakebook Team",
    location: "Hà Nội",
    mutualFriendsCount: 42,
    friendshipStatus: "FRIEND",
  },
  {
    id: "user-minh-2",
    username: "vietminh",
    displayName: "Việt Minh",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
    bio: "Lập trình viên React & Java",
    education: "Đại học FPT",
    location: "Đà Nẵng",
    mutualFriendsCount: 8,
    friendshipStatus: "NONE",
  },
];

export async function searchUsers(query: string, page = 0, size = 20): Promise<{ content: UserSearchResult[]; totalElements: number }> {
  if (!query || query.trim() === "") {
    return { content: [], totalElements: 0 };
  }

  try {
    const response = await api.get("/services/userservice/api/user-profiles/search", {
      params: { query: query.trim(), page, size },
    });
    return {
      content: response.data.content || response.data,
      totalElements: response.data.totalElements ?? response.data.length ?? 0,
    };
  } catch (err) {
    console.info("Backend search API not ready yet or returned error, using fallback mock search data.", err);
    const q = query.toLowerCase().trim();
    const filtered = MOCK_SEARCH_DATABASE.filter(
      (u) => u.displayName.toLowerCase().includes(q) || u.username.toLowerCase().includes(q)
    );
    return {
      content: filtered,
      totalElements: filtered.length,
    };
  }
}

export async function sendFriendRequest(targetUserId: string): Promise<boolean> {
  try {
    await api.post("/services/userservice/api/friend-requests", {
      receiverId: targetUserId,
    });
    return true;
  } catch (err) {
    console.info("Backend friend request API error, applying optimistic UI update.", err);
    return true;
  }
}

export async function cancelFriendRequest(targetUserId: string): Promise<boolean> {
  try {
    await api.delete(`/services/userservice/api/friend-requests/target/${targetUserId}`);
    return true;
  } catch (err) {
    console.info("Backend cancel friend request API error, applying optimistic UI update.", err);
    return true;
  }
}
