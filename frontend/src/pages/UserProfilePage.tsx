import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import ProfileLayout, { type ProfileUser } from "@/components/profile/ProfileLayout";
import { getUserProfileDetails, type UserProfileDetail } from "@/services/profileService";
import {
  getCurrentUserProfile,
  getAllFriends,
  sendFriendRequest,
  cancelFriendRequest,
  acceptFriendRequestById,
  rejectFriendRequest,
  unfriend,
  getUserAvatarUrl,
} from "@/services/friendsService";
import { usePostStore } from "@/stores/postStore";
import type { FriendUser } from "@/types";

export default function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { posts } = usePostStore();

  const [profileDetail, setProfileDetail] = useState<UserProfileDetail | null>(null);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [friends, setFriends] = useState<FriendUser[]>([]);
  const [mutualFriends, setMutualFriends] = useState<FriendUser[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [showFriendMenu, setShowFriendMenu] = useState<boolean>(false);

  // 1. Fetch current logged-in user profile
  useEffect(() => {
    getCurrentUserProfile()
      .then((me) => setCurrentUserId(me.id))
      .catch((err) => console.error("Không thể lấy thông tin tài khoản hiện tại:", err));
  }, []);

  // 2. Fetch target user profile details and target user's friends list
  useEffect(() => {
    if (!userId) {
      setError("Không tìm thấy thông tin ID người dùng.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    Promise.all([
      getUserProfileDetails(userId),
      getAllFriends(userId).catch(() => []),
    ])
      .then(([detail, targetFriendships]) => {
        setProfileDetail(detail);

        // Format target user's friends
        const formattedTargetFriends: FriendUser[] = targetFriendships.map((f) => {
          const friendUser = f.friend?.id === userId ? f.user : f.friend;
          return {
            id: friendUser.id,
            name: friendUser.displayName || friendUser.username || "Người dùng",
            avatar: getUserAvatarUrl(friendUser),
            cover: "/default-cover.svg",
            mutualFriends: 0,
            location: friendUser.location || "",
            work: friendUser.work || "",
            education: friendUser.education || "",
            bio: friendUser.bio || "",
          };
        });

        setFriends(formattedTargetFriends);
      })
      .catch((err) => {
        console.error("Lỗi khi tải thông tin hồ sơ người dùng:", err);
        setError("Không thể tải thông tin hồ sơ người dùng.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [userId]);

  // 3. Compute real mutual friends if currentUserId & targetUserId exist
  useEffect(() => {
    if (!currentUserId || !userId || currentUserId === userId || friends.length === 0) {
      setMutualFriends([]);
      return;
    }

    getAllFriends(currentUserId)
      .then((myFriendships) => {
        const myFriendIds = new Set(
          myFriendships.map((f) => (f.friend?.id === currentUserId ? f.user?.id : f.friend?.id))
        );
        const actualMutual = friends.filter((f) => myFriendIds.has(String(f.id)));
        setMutualFriends(actualMutual);
      })
      .catch(console.error);
  }, [currentUserId, userId, friends]);

  // Handle action buttons (Thêm bạn bè / Hủy lời mời / Chấp nhận / Bạn bè)
  const handleToggleFriend = async () => {
    if (!profileDetail || !currentUserId) return;
    const currentStatus = profileDetail.friendshipStatus;

    if (currentStatus === "NONE") {
      setProfileDetail({ ...profileDetail, friendshipStatus: "PENDING_SENT" });
      try {
        const req = await sendFriendRequest(currentUserId, profileDetail.id);
        if (req && req.id) {
          setProfileDetail((prev) => (prev ? { ...prev, friendRequestId: req.id } : prev));
        }
      } catch {
        setProfileDetail((prev) => (prev ? { ...prev, friendshipStatus: "NONE" } : prev));
      }
    } else if (currentStatus === "PENDING_SENT") {
      setProfileDetail({ ...profileDetail, friendshipStatus: "NONE" });
      try {
        if (profileDetail.friendRequestId) {
          await cancelFriendRequest(profileDetail.friendRequestId);
        }
      } catch {
        setProfileDetail((prev) => (prev ? { ...prev, friendshipStatus: "PENDING_SENT" } : prev));
      }
    } else if (currentStatus === "PENDING_RECEIVED") {
      if (!profileDetail.friendRequestId) return;
      setProfileDetail({ ...profileDetail, friendshipStatus: "FRIEND" });
      try {
        await acceptFriendRequestById(profileDetail.friendRequestId, currentUserId, profileDetail.id);
        // Refresh friends list after accepting
        getAllFriends(profileDetail.id).then((targetFriendships) => {
          const formattedTargetFriends: FriendUser[] = targetFriendships.map((f) => {
            const friendUser = f.friend?.id === profileDetail.id ? f.user : f.friend;
            return {
              id: friendUser.id,
              name: friendUser.displayName || friendUser.username || "Người dùng",
              avatar: getUserAvatarUrl(friendUser),
              cover: "/default-cover.svg",
              mutualFriends: 0,
              location: friendUser.location || "",
              work: friendUser.work || "",
              education: friendUser.education || "",
              bio: friendUser.bio || "",
            };
          });
          setFriends(formattedTargetFriends);
        });
      } catch {
        setProfileDetail((prev) => (prev ? { ...prev, friendshipStatus: "PENDING_RECEIVED" } : prev));
      }
    }
  };

  // Handle Decline / Reject Friend Request
  const handleRejectRequest = async () => {
    if (!profileDetail || !profileDetail.friendRequestId) return;
    setProfileDetail({ ...profileDetail, friendshipStatus: "NONE" });
    try {
      await rejectFriendRequest(profileDetail.friendRequestId);
    } catch {
      setProfileDetail((prev) => (prev ? { ...prev, friendshipStatus: "PENDING_RECEIVED" } : prev));
    }
  };

  // Handle Unfriend action
  const handleUnfriend = async () => {
    if (!profileDetail || !currentUserId) return;
    if (!window.confirm(`Bạn có chắc chắn muốn hủy kết bạn với ${profileDetail.displayName}?`)) return;

    setShowFriendMenu(false);
    setProfileDetail({ ...profileDetail, friendshipStatus: "NONE" });

    try {
      await unfriend(profileDetail.id);
      // Update target user's friends list
      const targetFriendships = await getAllFriends(profileDetail.id);
      const formattedTargetFriends: FriendUser[] = targetFriendships.map((f) => {
        const friendUser = f.friend?.id === profileDetail.id ? f.user : f.friend;
        return {
          id: friendUser.id,
          name: friendUser.displayName || friendUser.username || "Người dùng",
          avatar: getUserAvatarUrl(friendUser),
          cover: "/default-cover.svg",
          mutualFriends: 0,
          location: friendUser.location || "",
          work: friendUser.work || "",
          education: friendUser.education || "",
          bio: friendUser.bio || "",
        };
      });
      setFriends(formattedTargetFriends);
    } catch (err) {
      console.error("Lỗi khi hủy kết bạn:", err);
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

  const isOwn = profileDetail.friendshipStatus === "SELF" || (currentUserId !== null && currentUserId === profileDetail.id);

  const user: ProfileUser = {
    name: profileDetail.displayName || profileDetail.username || "Người dùng",
    avatar: getUserAvatarUrl({
      id: profileDetail.id,
      displayName: profileDetail.displayName,
      avatarMediaId: profileDetail.avatarMediaId,
    }),
    cover: profileDetail.coverMediaId
      ? `/services/mediaservice/api/media/${profileDetail.coverMediaId}`
      : "/default-cover.svg",
    bio: profileDetail.bio,
    location: profileDetail.location,
    education: profileDetail.education,
    work: profileDetail.workplace,
    relationship: profileDetail.relationship,
  };

  // Target user's posts filter
  const targetUserPosts = posts.filter(
    (p) =>
      p.user === profileDetail.displayName ||
      p.user === profileDetail.username ||
      p.user === user.name
  );

  const actionButtons = isOwn ? (
    <button
      onClick={() => navigate("/profile")}
      className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
    >
      <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" />
      </svg>
      Chỉnh sửa trang cá nhân
    </button>
  ) : (
    <div className="flex gap-2">
      {profileDetail.friendshipStatus === "FRIEND" && (
        <div className="relative">
          <button
            onClick={() => setShowFriendMenu(!showFriendMenu)}
            className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            <svg className="w-4 h-4 text-[#1877F2]" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
            Bạn bè
          </button>

          {showFriendMenu && (
            <div className="absolute right-0 mt-1 w-48 bg-white rounded-xl shadow-lg border border-[#E4E6EB] py-1 z-20 animate-fade-in">
              <button
                onClick={handleUnfriend}
                className="w-full text-left px-4 py-2.5 text-sm font-semibold text-red-600 hover:bg-[#F0F2F5] transition-colors flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.366zm1.414-1.414L6.525 5.11a6 6 0 018.366 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clipRule="evenodd" />
                </svg>
                Hủy kết bạn
              </button>
            </div>
          )}
        </div>
      )}

      {profileDetail.friendshipStatus === "PENDING_SENT" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
          </svg>
          Hủy lời mời
        </button>
      )}

      {profileDetail.friendshipStatus === "PENDING_RECEIVED" && (
        <div className="flex gap-2">
          <button
            onClick={handleToggleFriend}
            className="flex items-center gap-2 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            Chấp nhận lời mời
          </button>
          <button
            onClick={handleRejectRequest}
            className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            Từ chối
          </button>
        </div>
      )}

      {profileDetail.friendshipStatus === "NONE" && (
        <button
          onClick={handleToggleFriend}
          className="flex items-center gap-2 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 00-6 6h12a6 6 0 00-6-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 102 0v-1h1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
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
      friends={friends}
      mutualCount={mutualFriends.length || profileDetail.mutualFriendsCount}
      mutualFriends={mutualFriends}
      actionButtons={actionButtons}
    />
  );
}
