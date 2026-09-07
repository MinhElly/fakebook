import { useState } from "react";
import { CommentContext } from "@/stores/commentStore";
import type { Comment } from "@/types";

export default function CommentProvider({ children }: { children: React.ReactNode }) {
  const [comments] = useState<Comment[]>([]);

  function createComment(postId: number, content: string, parentId: number | null = null) {
    void postId; void content; void parentId;
  }

  function updateComment(id: number, content: string) {
    void id; void content;
  }

  function deleteComment(id: number) {
    // Delete comment and all its replies
    void id;
  }

  function toggleLike(id: number) {
    void id;
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
