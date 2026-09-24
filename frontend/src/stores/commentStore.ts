import { createContext, useContext } from "react";
import type { Comment } from "@/types";

export interface CommentState {
  comments: Comment[];
  createComment: (postId: string, content: string, parentId?: string | null) => Promise<string | null>;
  updateComment: (id: string, content: string) => Promise<void>;
  deleteComment: (id: string) => Promise<void>;
  toggleLike: (id: string) => Promise<void>;
  getPostComments: (postId: string) => Comment[];
  getReplies: (parentId: string) => Comment[];
  fetchComments: (postId: string) => Promise<void>;
  fetchedPosts: Set<string>;
}

export const CommentContext = createContext<CommentState>({
  comments: [],
  createComment: async () => null,
  updateComment: async () => {},
  deleteComment: async () => {},
  toggleLike: async () => {},
  getPostComments: () => [],
  getReplies: () => [],
  fetchComments: async () => {},
  fetchedPosts: new Set(),
});

export function useCommentStore() {
  return useContext(CommentContext);
}
