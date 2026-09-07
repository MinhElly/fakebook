import { useState } from "react";
import { UserContext, type UserProfile } from "@/stores/userStore";

const DEFAULT_PROFILE: UserProfile = {
  name: "Nguyễn Văn An",
  avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=80&h=80&fit=crop&auto=format",
  cover: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=900&h=300&fit=crop&auto=format",
  location: "Hà Nội",
  education: "Đại học Bách Khoa HN",
  work: "FPT Software",
  relationship: "Độc thân",
  bio: "Yêu lập trình, thích du lịch và khám phá những điều mới 🌍",
};

export default function UserProvider({ children }: { children: React.ReactNode }) {
  const [profile, setProfile] = useState<UserProfile>(DEFAULT_PROFILE);

  function updateProfile(data: Partial<UserProfile>) {
    setProfile(prev => ({ ...prev, ...data }));
  }

  return (
    <UserContext.Provider value={{ profile, updateProfile }}>
      {children}
    </UserContext.Provider>
  );
}
