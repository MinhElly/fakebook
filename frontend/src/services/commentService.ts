import api from "./apis";

export interface CommentDTO {
  id: string;
  postId: string;
  authorId: string;
  content: string;
  status?: string;
  parentComment?: { id: string } | null;
  createdAt?: string;
  editedAt?: string | null;
  editCount?: number;
  
  // Custom fields typically returned or populated
  authorName?: string;
  authorAvatar?: string;
  likeCount?: number;
  likedByCurrentUser?: boolean;
}

export interface CommentPreview {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
  editedAt?: string | null;
  editCount?: number;
}

export interface CommentSummary {
  postId: string;
  commentCount: number;
  previewComment: CommentPreview | null;
}

export async function fetchCommentSummaries(postIds: string[]): Promise<CommentSummary[]> {
  const uniquePostIds = [...new Set(postIds)].slice(0, 50);
  if (uniquePostIds.length === 0) return [];

  const response = await api.post<CommentSummary[]>("/services/commentservice/api/comments/summaries", {
    postIds: uniquePostIds,
  });
  return response.data;
}

export async function getCommentsByPostId(postId: string): Promise<CommentDTO[]> {
  try {
    const comments: CommentDTO[] = [];
    const size = 100;
    for (let page = 0; ; page += 1) {
      const response = await api.get<CommentDTO[]>("/services/commentservice/api/comments", {
        params: { "postId.equals": postId, size, page, sort: ["createdAt,asc", "id,asc"] },
        paramsSerializer: { indexes: null },
      });
      const batch = response.data || [];
      comments.push(...batch);
      const total = Number(response.headers["x-total-count"]);
      if (batch.length < size || (Number.isFinite(total) && comments.length >= total)) break;
    }
    return comments;
  } catch (error) {
    console.warn("Failed to fetch comments:", error);
    throw error;
  }
}

export async function createComment(postId: string, content: string, parentId?: string | null): Promise<CommentDTO | null> {
  try {
    if (parentId) {
      const response = await api.post<CommentDTO>("/services/commentservice/api/comments/reply", {
        parentCommentId: parentId,
        content,
      });
      return response.data;
    } else {
      const response = await api.post<CommentDTO>("/services/commentservice/api/comments/create", {
        postId,
        content,
      });
      return response.data;
    }
  } catch (error) {
    console.error("Failed to create comment:", error);
    return null;
  }
}

export async function deleteComment(commentId: string): Promise<boolean> {
  try {
    await api.delete(`/services/commentservice/api/comments/${commentId}`);
    return true;
  } catch (error) {
    console.error("Failed to delete comment:", error);
    return false;
  }
}

export async function updateComment(commentId: string, content: string): Promise<CommentDTO | null> {
  try {
    const response = await api.put<CommentDTO>(`/services/commentservice/api/comments/${commentId}`, {
      content,
    });
    return response.data;
  } catch (error) {
    console.error("Failed to update comment:", error);
    return null;
  }
}

export interface CommentRevision {
  revision: number;
  content: string;
  editorId: string;
  editedAt: string | null;
}

export async function getCommentHistory(id: string, before?: number): Promise<CommentRevision[]> {
  const { data } = await api.get<CommentRevision[]>(`/services/commentservice/api/comments/${id}/history`, { params: { before, size: 20 } });
  return data;
}
