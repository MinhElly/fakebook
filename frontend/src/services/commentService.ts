import api from "./apis";

export interface CommentDTO {
  id: string;
  postId: string;
  authorId: string;
  content: string;
  parentId?: string | null;
  createdAt: string;
}

/**
 * Fetch comments for a specific post.
 */
export async function getCommentsByPostId(postId: string): Promise<CommentDTO[]> {
  try {
    const response = await api.get<CommentDTO[]>(`/services/commentservice/api/comments/post/${postId}`);
    return response.data || [];
  } catch (error) {
    console.warn("Failed to fetch comments from commentService:", error);
    return [];
  }
}

/**
 * Create a new comment on a post.
 */
export async function createComment(postId: string, content: string, parentId?: string | null): Promise<CommentDTO | null> {
  try {
    const response = await api.post<CommentDTO>("/services/commentservice/api/comments", {
      postId,
      content,
      parentId: parentId || null,
    });
    return response.data;
  } catch (error) {
    console.error("Failed to create comment:", error);
    return null;
  }
}

/**
 * Delete a comment by ID.
 */
export async function deleteComment(commentId: string): Promise<boolean> {
  try {
    await api.delete(`/services/commentservice/api/comments/${commentId}`);
    return true;
  } catch (error) {
    console.error("Failed to delete comment:", error);
    return false;
  }
}
