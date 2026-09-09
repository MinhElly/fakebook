export interface User {
  name: string;
  avatar: string;
  cover: string;
}

export interface Story {
  id: number;
  name: string;
  avatar: string;
  bg: string;
  isOwn?: boolean;
}

export interface Post {
  id: number;
  user: string;
  avatar: string;
  time: string;
  content: string;
  image: string | null;
  likes: number;
  comments: number;
  shares: number;
  liked: boolean;
}

export interface Message {
  id: number;
  name: string;
  avatar: string;
  text: string;
  time: string;
  online: boolean;
  unread: number;
}

export interface Notification {
  id: number;
  avatar: string;
  text: string;
  time: string;
  read: boolean;
}

export interface Contact {
  id: number;
  name: string;
  avatar: string;
  online: boolean;
}

export type DropdownType = "messages" | "notifications" | "profile" | null;

export interface FriendUser {
  id: string | number;
  name: string;
  avatar: string;
  cover: string;
  mutualFriends: number;
  location: string;
  work: string;
  education: string;
  bio: string;
}

export type FriendStatus = "none" | "pending_sent" | "pending_received" | "friends";

export interface FriendRequest {
  id: string | number;
  from: FriendUser;
  time: string;
}

export interface Comment {
  id: number;
  postId: number;
  parentId: number | null;
  user: string;
  avatar: string;
  content: string;
  time: string;
  likes: number;
  liked: boolean;
}
