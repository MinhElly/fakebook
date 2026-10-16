import { ProfileSkeleton } from "@/components/ui/Skeleton";
import LoadError from "@/components/ui/LoadError";
import { useState, useEffect } from "react";
import EditProfileModal from "@/components/profile/EditProfileModal";
import ProfileLayout from "@/components/profile/ProfileLayout";
import useProfilePosts from "@/hooks/useProfilePosts";
import { useUserStore } from "@/stores/userStore";
import { getAllFriends, getUserAvatarUrl } from "@/services/friendsService";
import type { FriendUser } from "@/types";
import { useAuth } from "@/providers/AuthProvider";

export default function ProfilePage() {
  const { user } = useAuth();
  const { posts, loadMorePosts, hasMore, error: feedError, loading } = useProfilePosts(user?.id);
  const { profile, loading: profileLoading, error: profileError, retry: retryProfile } = useUserStore();
  const [friendsLoading, setFriendsLoading] = useState(true);
  const [friendsError, setFriendsError] = useState<string | null>(null);
  const [friendsAttempt, setFriendsAttempt] = useState(0);
  const [friends, setFriends] = useState<FriendUser[]>([]);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    let active = true;
    setFriendsLoading(true); setFriendsError(null);
    if (!user?.id) { setFriendsLoading(false); return; }
    getAllFriends(user.id).then(friendships => {
      const formatted: FriendUser[] = friendships.map(f => {
        const friend = f.friend?.id === user.id ? f.user : f.friend;
        return { id: friend.id, name: friend.displayName || friend.username || "Người dùng", avatar: getUserAvatarUrl(friend), cover: "/default-cover.svg", mutualFriends: 0, location: friend.location || "", work: friend.work || "", education: friend.education || "", bio: friend.bio || "" };
      });
      if (active) setFriends(formatted);
    })
      .catch(() => { if (active) setFriendsError("Không tải được danh sách bạn bè."); })
      .finally(() => { if (active) setFriendsLoading(false); });
    return () => { active = false; };
  }, [user?.id, friendsAttempt]);

  // Bắt sự kiện cuộn chuột để làm Infinity Scroll giống Feed
  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 200) {
        if (hasMore && !loading) {
          loadMorePosts();
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [hasMore, loading]);

  if (profileLoading) return <ProfileSkeleton />;
  if (profileError) return <LoadError message={profileError} onRetry={retryProfile} />;

  const myPosts = posts;

  const actionButtons = (
    <>
      <button
        disabled
        title="Cần kết nối stories service"
        className="flex cursor-not-allowed items-center gap-2 rounded-lg bg-[#E4E6EB] px-4 py-2 text-sm font-semibold text-[#65676B]"
      >
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
          <path
            fillRule="evenodd"
            d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
            clipRule="evenodd"
          />
        </svg>
        Thêm vào tin
      </button>
      <button
        onClick={() => setShowEditModal(true)}
        className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
      >
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" />
        </svg>
        Chỉnh sửa trang cá nhân
      </button>
    </>
  );

  return (
    <>
      <ProfileLayout
        friendsLoading={friendsLoading}
        friendsError={friendsError}
        retryFriends={() => setFriendsAttempt(n => n + 1)}
        user={profile}
        isOwn={true}
        posts={myPosts}
        feedError={feedError} retryFeed={loadMorePosts}
        friends={friends}
        actionButtons={actionButtons}
        onEditCover={() => setShowEditModal(true)}
        onEditAvatar={() => setShowEditModal(true)}
        loading={loading}
        hasMore={hasMore}
      />
      {showEditModal && <EditProfileModal onClose={() => setShowEditModal(false)} />}
    </>
  );
}
