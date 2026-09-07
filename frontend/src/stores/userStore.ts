import { createContext, useContext } from "react";

export interface UserProfile {
  name: string;
  avatar: string;
  cover: string;
  location: string;
  education: string;
  work: string;
  relationship: string;
  bio: string;
}

export interface UserState {
  profile: UserProfile;
  updateProfile: (data: Partial<UserProfile>) => void;
}

export const UserContext = createContext<UserState>({
  profile: {
    name: "Nguyễn Văn An",
    avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=80&h=80&fit=crop&auto=format",
    cover: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=900&h=300&fit=crop&auto=format",
    location: "Hà Nội",
    education: "Đại học Bách Khoa HN",
    work: "FPT Software",
    relationship: "Độc thân",
    bio: "",
  },
  updateProfile: () => {},
});

export function useUserStore() {
  return useContext(UserContext);
}
