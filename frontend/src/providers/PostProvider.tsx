import { useState } from "react";
import { PostContext } from "@/stores/postStore";
import { POSTS } from "@/constants/data";
import type { Post } from "@/types";

let nextId = POSTS.length + 1;

export default function PostProvider({ children }: { children: React.ReactNode }) {
  const [posts, setPosts] = useState<Post[]>(POSTS);

  function addPost(content: string, image: string | null) {
    const newPost: Post = {
      id: nextId++,
      user: "Nguyễn Văn An",
      avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=80&h=80&fit=crop&auto=format",
      time: "Vừa xong",
      content,
      image,
      likes: 0,
      comments: 0,
      shares: 0,
      liked: false,
    };
    setPosts(prev => [newPost, ...prev]);
  }

  function updatePost(id: number, content: string, image: string | null) {
    setPosts(prev => prev.map(p => p.id === id ? { ...p, content, image } : p));
  }

  function deletePost(id: number) {
    setPosts(prev => prev.filter(p => p.id !== id));
  }

  return (
    <PostContext.Provider value={{ posts, addPost, updatePost, deletePost }}>
      {children}
    </PostContext.Provider>
  );
}
