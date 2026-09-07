import { useState, useEffect } from "react";
import { UserContext, type UserProfile } from "@/stores/userStore";
import { useAuth } from "@/providers/AuthProvider";
import api from "@/services/apis";

const DEFAULT_AVATAR = "/default-avatar.svg";
const DEFAULT_COVER = "/default-cover.svg";

export default function UserProvider({ children }: { children: React.ReactNode }) {
  const { status, user } = useAuth();
  const [profile, setProfile] = useState<UserProfile>({
    name: user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : (user?.username || "Người dùng"),
    avatar: DEFAULT_AVATAR,
    cover: DEFAULT_COVER,
    location: "",
    education: "",
    work: "",
    relationship: "",
    bio: "",
  });

  useEffect(() => {
    if (status !== "authenticated") return;

    api.get("/user-profiles/me")
      .then(({ data }) => {
        const fullName = data.displayName ||
          (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : (user?.username || user?.email));

        setProfile((prev) => ({
          ...prev,
          name: fullName || prev.name,
          bio: data.bio || prev.bio,
          avatar: data.avatarMediaId ? `/api/media/${data.avatarMediaId}` : prev.avatar,
          cover: data.coverMediaId ? `/api/media/${data.coverMediaId}` : prev.cover,
        }));
      })
      .catch(() => {
        if (user) {
          const fallbackName = (user.firstName && user.lastName)
            ? `${user.firstName} ${user.lastName}`
            : (user.username || user.email);
          if (fallbackName) {
            setProfile((prev) => ({ ...prev, name: fallbackName }));
          }
        }
      });
  }, [status, user]);

  async function updateProfile(data: Partial<UserProfile>) {
    setProfile((prev) => ({ ...prev, ...data }));

    if (status === "authenticated") {
      try {
        await api.patch("/user-profiles/me", {
          displayName: data.name,
          bio: data.bio,
        });
      } catch (err) {
        console.error("Failed to sync profile update with backend:", err);
      }
    }
  }

  return (
    <UserContext.Provider value={{ profile, updateProfile }}>
      {children}
    </UserContext.Provider>
  );
}
