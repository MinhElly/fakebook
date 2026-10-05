import api from "./apis";

export interface FeedItemDTO {
  id: string;
  userId: string;
  postId: string;
  createdAt: string;
}

/**
 * Fetch personalized news feed items for current user.
 */
export async function getPersonalizedFeed(page = 0, size = 10): Promise<FeedItemDTO[]> {
  const response = await api.get<FeedItemDTO[]>(
    `/services/feedservice/api/feed/me?page=${page}&size=${size}&sort=createdAt,desc`
  );
  return response.data || [];
}
