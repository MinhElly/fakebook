import api from "./apis"

export interface FeedItemDTO {
  postId: string
  source: "FRIENDS" | "FOLLOWING" | "DISCOVERY"
}

export interface FeedPage {
  items: FeedItemDTO[]
  nextCursor: string | null
  hasMore: boolean
  loginRequired: boolean
}

export async function getFeed(
  mode: "home" | "friends",
  cursor?: string | null,
): Promise<FeedPage> {
  const response = await api.get<FeedPage>(
    `/services/feedservice/api/feed/${mode}`,
    {
      params: cursor ? { cursor } : {},
      withCredentials: true,
    },
  )

  return response.data
}
