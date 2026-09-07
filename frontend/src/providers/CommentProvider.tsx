import { useState } from "react";
import { CommentContext } from "@/stores/commentStore";
import { COMMENTS } from "@/constants/data";
import { useUserStore } from "@/stores/userStore";
import type { Comment } from "@/types";

let nextId = COMMENTS.length + 1;

function now() {
  return "Vừa xong";
}

export default function CommentProvider({ children }: { children: React.ReactNode }) {
  const [comments, setComments] = useState<Comment[]>(COMMENTS);
  const { profile } = useUserStore();

  function createComment(postId: number, content: string, parentId: number | null = null) {
    const c: Comment = {
      id: nextId++,
      postId,
      parentId,
      user: profile.name,
      avatar: profile.avatar,
      content: content.trim(),
      time: now(),
      likes: 0,
      liked: false,
    };
    setComments(prev => [...prev, c]);
  }

  function updateComment(id: number, content: string) {
    setComments(prev => prev.map(c => c.id === id ? { ...c, content: content.trim() } : c));
  }

  function deleteComment(id: number) {
    // Delete comment and all its replies
    setComments(prev => prev.filter(c => c.id !== id && c.parentId !== id));
  }

  function toggleLike(id: number) {
    setComments(prev => prev.map(c =>
      c.id === id ? { ...c, liked: !c.liked, likes: c.liked ? c.likes - 1 : c.likes + 1 } : c
    ));
  }

  function getPostComments(postId: number) {
    return comments.filter(c => c.postId === postId && c.parentId === null);
  }

  function getReplies(parentId: number) {
    return comments.filter(c => c.parentId === parentId);
  }

  return (
    <CommentContext.Provider value={{ comments, createComment, updateComment, deleteComment, toggleLike, getPostComments, getReplies }}>
      {children}
    </CommentContext.Provider>
  );
}
