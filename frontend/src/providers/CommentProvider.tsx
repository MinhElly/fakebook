import { useState, useCallback } from "react";
import { CommentContext } from "@/stores/commentStore";
import type { Comment } from "@/types";
import { getCommentsByPostId, createComment as apiCreateComment, updateComment as apiUpdateComment, deleteComment as apiDeleteComment } from "@/services/commentService";
import { useAuth } from "@/providers/AuthProvider";
import { getTimeAgo } from "@/utils/timeUtils";

export default function CommentProvider({ children }: { children: React.ReactNode }) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [fetchedPosts, setFetchedPosts] = useState<Set<string>>(new Set());
  const { user } = useAuth();

  const fetchComments = useCallback(async (postId: string) => {
    if (fetchedPosts.has(postId)) return;
    try {
      const dtos = await getCommentsByPostId(postId);
      
      // Fetch user profiles for comments
      const authorIdsSet = new Set<string>();
      dtos.forEach(dto => authorIdsSet.add(dto.authorId));
      const authorIds = Array.from(authorIdsSet);
      
      const profileMap: Record<string, any> = {};
      if (authorIds.length > 0) {
        try {
          const { default: api } = await import("@/services/apis");
          const idQuery = authorIds.map(id => `id.in=${id}`).join("&");
          const profileRes = await api.get(`/services/userservice/api/user-profiles/public?${idQuery}`, { timeout: 3000 });
          profileRes.data.forEach((p: any) => {
            profileMap[p.id] = {
              name: p.displayName || p.username || "Người dùng",
              avatar: p.avatarMediaId ? `/services/mediaservice/api/media/${p.avatarMediaId}/file` : "/default-avatar.svg"
            };
          });
        } catch (e) {
          console.warn("UserService tắt hoặc không phản hồi.");
        }
      }

      const newComments: Comment[] = dtos.map(dto => ({
        id: dto.id,
        postId: dto.postId,
        parentId: dto.parentComment?.id || null,
        authorId: dto.authorId,
        user: profileMap[dto.authorId]?.name || "Người dùng",
        avatar: profileMap[dto.authorId]?.avatar || "/default-avatar.svg",
        content: dto.content,
        time: dto.createdAt ? getTimeAgo(dto.createdAt) : "Vừa xong",
        timestamp: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
        likes: dto.likeCount || 0,
        liked: dto.likedByCurrentUser || false,
      }));
      
      setComments(prev => {
        const filtered = prev.filter(c => c.postId !== postId);
        return [...filtered, ...newComments];
      });
      setFetchedPosts(prev => new Set(prev).add(postId));
    } catch (e) {
      console.error(e);
    }
  }, [fetchedPosts]);

  async function createComment(postId: string, content: string, parentId: string | null = null) {
    const dto = await apiCreateComment(postId, content, parentId);
    if (dto) {
      const newComment: Comment = {
        id: dto.id,
        postId: dto.postId,
        parentId: dto.parentComment?.id || null,
        authorId: dto.authorId || user?.id || "",
        user: user?.firstName || user?.username || "Bạn",
        avatar: "/default-avatar.svg",
        content: dto.content,
        time: dto.createdAt ? getTimeAgo(dto.createdAt) : "Vừa xong",
        timestamp: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
        likes: 0,
        liked: false,
      };
      setComments(prev => [...prev, newComment]);
      return newComment.id;
    }
    return null;
  }

  async function updateComment(id: string, content: string) {
    const dto = await apiUpdateComment(id, content);
    if (dto) {
      setComments(prev => prev.map(c => c.id === id ? { ...c, content: dto.content } : c));
    }
  }

  async function deleteComment(id: string) {
    const success = await apiDeleteComment(id);
    if (success) {
      setComments(prev => prev.filter(c => c.id !== id && c.parentId !== id));
    }
  }

  async function toggleLike(id: string) {
    // Optimistic UI for now, actual implementation needs Reaction API
    setComments(prev => prev.map(c => {
      if (c.id === id) {
        return { ...c, liked: !c.liked, likes: c.liked ? Math.max(0, c.likes - 1) : c.likes + 1 };
      }
      return c;
    }));
  }

  function getPostComments(postId: string) {
    return comments.filter(c => c.postId === postId && !c.parentId);
  }

  function getReplies(parentId: string) {
    return comments.filter(c => c.parentId === parentId).sort((a, b) => a.timestamp - b.timestamp);
  }

  return (
    <CommentContext.Provider value={{ comments, createComment, updateComment, deleteComment, toggleLike, getPostComments, getReplies, fetchComments, fetchedPosts }}>
      {children}
    </CommentContext.Provider>
  );
}
