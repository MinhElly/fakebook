import api from "./apis";

export interface CommentDTO {
  id: string;
  postId: string;
  authorId: string;
  content: string;
  status?: string;
  parentComment?: { id: string } | null;
  createdAt?: string;
  
  // Custom fields typically returned or populated
  authorName?: string;
  authorAvatar?: string;
  likeCount?: number;
  likedByCurrentUser?: boolean;
}

export async function getCommentsByPostId(postId: string): Promise<CommentDTO[]> {
  try {
    const response = await api.get<CommentDTO[]>(`/services/commentservice/api/comments?postId.equals=${postId}&size=100`);
    return response.data || [];
  } catch (error) {
    console.warn("Failed to fetch comments:", error);
    return [];
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
