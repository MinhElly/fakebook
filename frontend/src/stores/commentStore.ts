import { createContext, useContext } from "react";
import type { Comment } from "@/types";

export interface CommentState {
  comments: Comment[];
  createComment: (postId: number, content: string, parentId?: number | null) => void;
  updateComment: (id: number, content: string) => void;
  deleteComment: (id: number) => void;
  toggleLike: (id: number) => void;
  getPostComments: (postId: number) => Comment[];
  getReplies: (parentId: number) => Comment[];
}

export const CommentContext = createContext<CommentState>({
  comments: [],
  createComment: () => {},
  updateComment: () => {},
  deleteComment: () => {},
  toggleLike: () => {},
  getPostComments: () => [],
  getReplies: () => [],
});

export function useCommentStore() {
  return useContext(CommentContext);
}
