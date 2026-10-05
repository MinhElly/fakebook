import api from "./apis";

export interface UserProfileDetail {
  id: string;
  username: string;
  displayName: string;
  avatarMediaId?: string;
  coverMediaId?: string;
  bio?: string;
  education?: string;
  workplace?: string;
  location?: string;
  relationship?: string;
  mutualFriendsCount: number;
  friendshipStatus: "SELF" | "FRIEND" | "PENDING_SENT" | "PENDING_RECEIVED" | "NONE";
  friendRequestId?: string;
}

export async function getUserProfileDetails(userId: string): Promise<UserProfileDetail> {
  const response = await api.get<UserProfileDetail>(`/services/userservice/api/user-profiles/${userId}/details`);
  return response.data;
}
