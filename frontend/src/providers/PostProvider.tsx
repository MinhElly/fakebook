import { useState } from "react";
import { PostContext } from "@/stores/postStore";
import type { Post } from "@/types";

export default function PostProvider({ children }: { children: React.ReactNode }) {
  const [posts] = useState<Post[]>([]);

  function addPost(_content: string, _image: string | null) {}
  function updatePost(_id: number, _content: string, _image: string | null) {}
  function deletePost(_id: number) {}

  return (
    <PostContext.Provider value={{ posts, addPost, updatePost, deletePost }}>
      {children}
    </PostContext.Provider>
  );
}
