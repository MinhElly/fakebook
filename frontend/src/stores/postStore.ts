import { createContext, useContext } from "react";
import type { Post } from "@/types";

export interface PostState {
  posts: Post[];
  loading: boolean;
  hasMore: boolean;
  isUploading: boolean;
  toastMessage: string | null;
  setToastMessage: (msg: string | null) => void;
  loadMorePosts: () => void;
  addPost: (content: string, image: File | string | null, visibility: string, taggedUserIds?: string[]) => Promise<void>;
  updatePost: (id: string, content: string, image: File | string | null, visibility: string, taggedUserIds?: string[]) => Promise<void>;
  deletePost: (id: string) => Promise<void>;
}

export const PostContext = createContext<PostState>({
  posts: [],
  loading: false,
  hasMore: true,
  isUploading: false,
  toastMessage: null,
  setToastMessage: () => {},
  loadMorePosts: () => { },
  addPost: async () => { },
  updatePost: async () => { },
  deletePost: async () => { },
});

export function usePostStore() {
  return useContext(PostContext);
}