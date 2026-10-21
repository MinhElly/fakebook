import { createContext, useContext } from "react";
import type { Post } from "@/types";

export interface PostState {
  posts: Post[];
  loading: boolean;
  hasMore: boolean;
  isUploading: boolean;
  pendingPost: any;
  toastMessage: string | null;
  setToastMessage: (msg: string | null) => void;
  loadMorePosts: () => void;
  addPost: (content: string, mediaIds: string[], visibility: string, taggedUserIds?: string[], publicationId?: string) => Promise<void>;
  updatePost: (id: string, content: string, mediaIds: string[] | undefined, visibility: string, taggedUserIds?: string[], version?: number, publicationId?: string) => Promise<void>;
  deletePost: (id: string) => Promise<void>;
  feedMode: "home" | "friends";
  changeFeedMode: (mode: "home" | "friends") => void;
  pauseFeed: () => void;
  feedError: string | null;
  feedExpired: boolean;
  retryFeed: () => void;
}

export const PostContext = createContext<PostState>({
  posts: [],
  loading: false,
  hasMore: true,
  isUploading: false,
  pendingPost: null,
  toastMessage: null,
  setToastMessage: () => {},
  loadMorePosts: () => { },
  addPost: async () => { },
  updatePost: async () => { },
  deletePost: async () => { },
  feedMode: "home", changeFeedMode: () => {}, pauseFeed: () => {}, feedError: null, feedExpired: false, retryFeed: () => {},
});

export function usePostStore() {
  return useContext(PostContext);
}
