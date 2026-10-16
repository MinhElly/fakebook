import { createContext, useContext } from "react";
import type { Comment } from "@/types";

export interface CommentState {
  comments: Comment[];
  createComment: (postId: string, content: string, parentId?: string | null) => Promise<string | null>;
  updateComment: (id: string, content: string) => Promise<boolean>;
  deleteComment: (id: string) => Promise<boolean>;
  toggleLike: (id: string) => Promise<void>;
  getPostComments: (postId: string) => Comment[];
  getReplies: (parentId: string) => Comment[];
  fetchComments: (postId: string, force?: boolean) => Promise<void>;
  fetchedPosts: Set<string>;
  loadingPosts: Set<string>;
  errors: Record<string, string>;
}

export const CommentContext = createContext<CommentState>({
  comments: [],
  createComment: async () => null,
  updateComment: async () => false,
  deleteComment: async () => false,
  toggleLike: async () => {},
  getPostComments: () => [],
  getReplies: () => [],
  fetchComments: async () => {},
  fetchedPosts: new Set(),
  loadingPosts: new Set(),
  errors: {},
});

export function useCommentStore() {
  return useContext(CommentContext);
}
