import { createContext, useContext } from "react";
import type { Post } from "@/types";

export interface PostState {
  posts: Post[];
  addPost: (content: string, image: string | null) => void;
  updatePost: (id: number, content: string, image: string | null) => void;
  deletePost: (id: number) => void;
}

export const PostContext = createContext<PostState>({
  posts: [],
  addPost: () => {},
  updatePost: () => {},
  deletePost: () => {},
});

export function usePostStore() {
  return useContext(PostContext);
}
