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
  try {
    const response = await api.get<FeedItemDTO[]>(
      `/services/feedservice/api/feed-items?sort=createdAt,desc&page=${page}&size=${size}`
    );
    return response.data || [];
  } catch (error) {
    console.error("Failed to fetch personalized feed from feedService:", error);
    return [];
  }
}
