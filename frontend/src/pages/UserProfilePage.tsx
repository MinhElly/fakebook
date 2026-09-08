import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import ProfileLayout, { type ProfileUser } from "@/components/profile/ProfileLayout";
import { getUserProfileDetails, type UserProfileDetail } from "@/services/profileService";
import { sendFriendRequest, cancelFriendRequest } from "@/services/searchService";
import { usePostStore } from "@/stores/postStore";
import type { FriendUser } from "@/types";

export default function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { posts } = usePostStore();

  const [profileDetail, setProfileDetail] = useState<UserProfileDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) {
      setError("Không tìm thấy thông tin ID người dùng.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getUserProfileDetails(userId)
      .then((detail) => {
        setProfileDetail(detail);
      })
      .catch((err) => {
        console.error("Lỗi khi tải thông tin hồ sơ:", err);
        setError("Không thể tải thông tin hồ sơ người dùng.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [userId]);

  // Handle action buttons (Thêm bạn bè / Hủy lời mời / Đồng ý)
  const handleToggleFriend = async () => {
    if (!profileDetail) return;
    const currentStatus = profileDetail.friendshipStatus;

    if (currentStatus === "NONE") {
      setProfileDetail({ ...profileDetail, friendshipStatus: "PENDING_SENT" });
      await sendFriendRequest(profileDetail.id);
    } else if (currentStatus === "PENDING_SENT") {
      setProfileDetail({ ...profileDetail, friendshipStatus: "NONE" });
      await cancelFriendRequest(profileDetail.id);
    }
  };

  if (loading) {
    return (
      <main className="min-h-[calc(100vh-56px)] bg-[#F0F2F5] px-4 py-12 flex justify-center items-center">
        <div className="bg-white p-8 rounded-xl shadow-sm text-center">
          <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-[#1877F2] border-t-transparent mb-3"></div>
          <p className="text-sm font-semibold text-[#65676B]">Đang tải hồ sơ người dùng...</p>
        </div>
      </main>
    );
  }

  if (error || !profileDetail) {
    return (
      <main className="min-h-[calc(100vh-56px)] bg-[#F0F2F5] px-4 py-8">
        <div className="mx-auto max-w-[720px] rounded-xl border border-[#E4E6EB] bg-white p-8 text-center">
          <p className="text-5xl mb-3">👤</p>
          <h2 className="text-xl font-bold text-[#1C1E21]">{error || "Không tìm thấy hồ sơ người dùng"}</h2>
          <p className="text-sm text-[#65676B] mt-1 mb-6">Trang này có thể đã bị xóa hoặc đường dẫn không đúng.</p>
          <button
            onClick={() => navigate(-1)}
            className="rounded-lg bg-[#1877F2] hover:bg-[#166FE5] text-white px-5 py-2.5 text-sm font-semibold transition-colors"
          >
            Quay lại
          </button>
        </div>
      </main>
    );
  }

  const isOwn = profileDetail.friendshipStatus === "SELF";

  const user: ProfileUser = {
    name: profileDetail.displayName,
    avatar: profileDetail.avatarMediaId ? `/api/media/${profileDetail.avatarMediaId}` : "/default-avatar.svg",
    cover: profileDetail.coverMediaId ? `/api/media/${profileDetail.coverMediaId}` : "/default-cover.svg",
    bio: profileDetail.bio,
    location: profileDetail.location,
    education: profileDetail.education,
    work: profileDetail.workplace,
    relationship: profileDetail.relationship,
  };

  // User posts placeholder
  const targetUserPosts = posts.filter((p) => p.user === profileDetail.displayName);

  // Mock mutual friends list based on count
  const mutualFriendsMock: FriendUser[] = Array.from({ length: Math.min(profileDetail.mutualFriendsCount, 6) }).map((_, i) => ({
    id: `mutual-${i}`,
    name: `Bạn chung ${i + 1}`,
    avatar: `https://images.unsplash.com/photo-${1500000000000 + i * 10000}?w=150&auto=format&fit=crop&q=80`,
    mutualFriends: 0,
  }));

  const actionButtons = isOwn ? (
    <button
      onClick={() => navigate("/profile")}
      className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
    >
      Chỉnh sửa trang cá nhân
    </button>
  ) : (
    <div className="flex gap-2">
      {profileDetail.friendshipStatus === "FRIEND" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4 text-[#1C1E21]" fill="currentColor" viewBox="0 0 20 20">
            <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
          </svg>
          Bạn bè
        </button>
      )}

      {profileDetail.friendshipStatus === "PENDING_SENT" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          Hủy lời mời
        </button>
      )}

      {profileDetail.friendshipStatus === "PENDING_RECEIVED" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          Chấp nhận lời mời
        </button>
      )}

      {profileDetail.friendshipStatus === "NONE" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 00-6 6h12a6 6 0 00-6-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
          </svg>
          Thêm bạn bè
        </button>
      )}

      <button className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors">
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zm-4 0H9v2h2V9z" clipRule="evenodd" />
        </svg>
        Nhắn tin
      </button>
    </div>
  );

  return (
    <ProfileLayout
      user={user}
      isOwn={isOwn}
      posts={targetUserPosts}
      mutualCount={profileDetail.mutualFriendsCount}
      mutualFriends={mutualFriendsMock}
      actionButtons={actionButtons}
    />
  );
}
