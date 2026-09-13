import api from "./apis";
import { fetchUserProfileSummary, type UserSummary } from "./friendsService";

export interface FollowDTO {
  id?: string;
  createdAt?: string;
  follower?: UserSummary;
  following?: UserSummary;
}

// Enrich profile summary cho danh sách follow nếu bị thiếu thông tin
async function enrichFollowsList(list: FollowDTO[]): Promise<FollowDTO[]> {
  return Promise.all(
    list.map(async (item) => {
      let follower = item.follower;
      let following = item.following;
      if (follower && follower.id && (!follower.displayName || !follower.username)) {
        const enriched = await fetchUserProfileSummary(follower.id);
        follower = { ...follower, ...enriched };
      }
      if (following && following.id && (!following.displayName || !following.username)) {
        const enriched = await fetchUserProfileSummary(following.id);
        following = { ...following, ...enriched };
      }
      return { ...item, follower, following };
    })
  );
}

// Follow 1 người dùng theo targetUserId
export async function followUser(targetUserId: string): Promise<FollowDTO> {
  const response = await api.post<FollowDTO>(`/services/userservice/api/follows/user/${targetUserId}`);
  return response.data;
}

// Unfollow 1 người dùng theo targetUserId
export async function unfollowUser(targetUserId: string): Promise<void> {
  await api.delete(`/services/userservice/api/follows/user/${targetUserId}`);
}

// Lấy danh sách những người mình đang theo dõi (following)
export async function getMyFollowingList(): Promise<FollowDTO[]> {
  const response = await api.get<FollowDTO[]>("/services/userservice/api/follows/me/following", {
    params: { page: 0, size: 100 },
  });
  return enrichFollowsList(response.data || []);
}

// Lấy danh sách những người đang theo dõi mình (follower)
export async function getMyFollowerList(): Promise<FollowDTO[]> {
  const response = await api.get<FollowDTO[]>("/services/userservice/api/follows/me/follower", {
    params: { page: 0, size: 100 },
  });
  return enrichFollowsList(response.data || []);
}

// Lấy danh sách following của user bất kỳ
export async function getUserFollowingList(userId: string): Promise<FollowDTO[]> {
  const response = await api.get<FollowDTO[]>(`/services/userservice/api/follows/user/${userId}/following`, {
    params: { page: 0, size: 100 },
  });
  return enrichFollowsList(response.data || []);
}

// Lấy danh sách follower của user bất kỳ
export async function getUserFollowerList(userId: string): Promise<FollowDTO[]> {
  const response = await api.get<FollowDTO[]>(`/services/userservice/api/follows/user/${userId}/follower`, {
    params: { page: 0, size: 100 },
  });
  return enrichFollowsList(response.data || []);
}
