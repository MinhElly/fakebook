import api from "@/services/apis"

export type ReactionType = "LIKE" | "LOVE" | "HAHA" | "WOW" | "SAD" | "ANGRY"

export interface ReactionSummary {
  postId: string
  totalCount: number
  counts: Record<ReactionType, number>
  myReaction: ReactionType | null
}

export interface PostReactor {
  userId: string
  reactionType: ReactionType
  reactedAt: string
}

const BASE_URL = "/services/postservice/api/post-reactions"

export function emptyReactionSummary(postId: string): ReactionSummary {
  return {
    postId,
    totalCount: 0,
    counts: { LIKE: 0, LOVE: 0, HAHA: 0, WOW: 0, SAD: 0, ANGRY: 0 },
    myReaction: null,
  }
}

export async function setPostReaction(
  postId: string,
  reactionType: ReactionType,
): Promise<ReactionSummary> {
  const response = await api.put(`${BASE_URL}/posts/${postId}`, {
    reactionType,
  })
  return response.data
}

export async function removePostReaction(
  postId: string,
): Promise<ReactionSummary> {
  const response = await api.delete(`${BASE_URL}/posts/${postId}`)
  return response.data
}

export async function fetchReactionSummaries(
  postIds: string[],
): Promise<ReactionSummary[]> {
  if (postIds.length === 0) return []

  const response = await api.get(`${BASE_URL}/summaries`, {
    params: { "postId.in": postIds.join(",") },
  })
  return response.data
}

export async function fetchPostReactors(
  postId: string,
  page = 0,
  size = 20,
): Promise<PostReactor[]> {
  const response = await api.get(`${BASE_URL}/posts/${postId}`, {
    params: { page, size, sort: "createdAt,desc" },
  })
  return response.data
}
