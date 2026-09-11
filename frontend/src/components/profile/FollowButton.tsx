import { useState } from "react";
import { useFriendStore } from "@/stores/friendStore";
import type { FriendUser } from "@/types";

interface Props {
  user: FriendUser;
  size?: "sm" | "md";
}

export default function FollowButton({ user, size = "md" }: Props) {
  const { isFollowing, follow, unfollow } = useFriendStore();
  const following = isFollowing(user.id);
  const [loading, setLoading] = useState(false);
  const pad = size === "sm" ? "px-3 py-1.5 text-xs" : "px-4 py-2 text-sm";

  const handleToggleFollow = async () => {
    if (loading) return;
    setLoading(true);
    try {
      if (following) {
        await unfollow(user.id);
      } else {
        await follow(user);
      }
    } catch (error) {
      console.error("Lỗi khi thao tác theo dõi:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleToggleFollow}
      disabled={loading}
      className={`flex items-center gap-1.5 font-semibold rounded-lg transition-colors ${pad} ${
        following
          ? "bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21]"
          : "bg-[#1877F2] hover:bg-[#166FE5] text-white"
      } ${loading ? "opacity-75 cursor-not-allowed" : ""}`}
    >
      {loading ? (
        <span className="inline-block animate-spin w-4 h-4 border-2 border-current border-t-transparent rounded-full" />
      ) : following ? (
        <>
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
            <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
          </svg>
          Đang theo dõi
        </>
      ) : (
        <>
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
            <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
          </svg>
          Theo dõi
        </>
      )}
    </button>
  );
}
