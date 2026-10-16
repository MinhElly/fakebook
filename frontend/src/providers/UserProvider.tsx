import { useState, useEffect } from "react";
import { UserContext, type UserProfile } from "@/stores/userStore";
import { useAuth } from "@/providers/AuthProvider";
import api from "@/services/apis";
import { resolveApiUrl } from "@/config/runtime-config";

const DEFAULT_AVATAR = "/default-avatar.svg";
const DEFAULT_COVER = "/default-cover.svg";

export default function UserProvider({ children }: { children: React.ReactNode }) {
  const { status, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
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
    let active = true;
    if (status !== "authenticated") { setLoading(false); return; }
    setLoading(true); setError(null);

    api.get("/services/userservice/api/user-profiles/me")
      .then(({ data }) => {
        if (!active) return;
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
            ? resolveApiUrl(`/services/mediaservice/api/media/${data.avatarMediaId}/file`)
            : prev.avatar,
          cover: data.coverMediaId
            ? resolveApiUrl(`/services/mediaservice/api/media/${data.coverMediaId}/file`)
            : prev.cover,
        }));
      })
      .catch(() => {
        if (!active) return;
        setError("Không tải được hồ sơ của bạn.");
        if (user) {
          const fallbackName = (user.firstName && user.lastName)
            ? `${user.firstName} ${user.lastName}`
            : (user.username || user.email);
          if (fallbackName) {
            setProfile((prev) => ({ ...prev, name: fallbackName }));
          }
        }
      }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, user?.id, attempt]);

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
    <UserContext.Provider value={{ profile, updateProfile, loading, error, retry: () => setAttempt(n => n + 1) }}>
      {children}
    </UserContext.Provider>
  );
}
