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
    name: "Người dùng",
    avatar: "/default-avatar.svg",
    cover: "/default-cover.svg",
    location: "",
    education: "",
    work: "",
    relationship: "",
    bio: "",
  },
  updateProfile: () => {},
});

export function useUserStore() {
  return useContext(UserContext);
}
