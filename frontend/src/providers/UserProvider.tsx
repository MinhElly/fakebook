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

    api.get("/services/userservice/api/user-profiles/me")
      .then(({ data }) => {
        const fullName = data.displayName ||
          (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : (user?.username || user?.email));

        setProfile((prev) => ({
          ...prev,
          name: fullName || prev.name,
          bio: data.bio ?? prev.bio,
          birthday: data.birthday ?? prev.birthday,
          gender: data.gender ?? prev.gender,
          location: data.location ?? prev.location,
          education: data.education ?? prev.education,
          work: data.work ?? prev.work,
          relationship: data.relationship ?? prev.relationship,
          avatarMediaId: data.avatarMediaId,
          coverMediaId: data.coverMediaId,
          avatar: data.avatarMediaId
            ? `/services/mediaservice/api/media/${data.avatarMediaId}`
            : prev.avatar,
          cover: data.coverMediaId
            ? `/services/mediaservice/api/media/${data.coverMediaId}`
            : prev.cover,
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

  async function updateProfile(data: Partial<UserProfile>): Promise<boolean> {
    try {
      await api.patch("/services/userservice/api/user-profiles/me", {
        displayName: data.name,
        bio: data.bio,
        birthday: data.birthday,
        gender: data.gender,
        location: data.location,
        education: data.education,
        work: data.work,
        relationship: data.relationship,
        avatarMediaId: data.avatarMediaId,
        coverMediaId: data.coverMediaId,
      });
      // Chỉ cập nhật UI sau khi Backend xác nhận thành công (200 OK)
      setProfile((prev) => ({ ...prev, ...data }));
      return true;
    } catch (err) {
      console.error("Failed to sync profile update with backend:", err);
      throw err;
    }
  }

  return (
    <UserContext.Provider value={{ profile, updateProfile }}>
      {children}
    </UserContext.Provider>
  );
}
