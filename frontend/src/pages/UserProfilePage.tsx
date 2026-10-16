import { ProfileSkeleton } from "@/components/ui/Skeleton";
import useRequireLogin from "@/hooks/useRequireLogin";
import { useAuth } from "@/providers/AuthProvider";
import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router";
import ProfileLayout, { type ProfileUser } from "@/components/profile/ProfileLayout";
import FollowButton from "@/components/profile/FollowButton";
import { getUserProfileDetails, type UserProfileDetail } from "@/services/profileService";
import { resolveApiUrl } from "@/config/runtime-config";
import {
  getAllFriends,
  sendFriendRequest,
  cancelFriendRequest,
  acceptFriendRequestById,
  rejectFriendRequest,
  unfriend,
  getUserAvatarUrl,
} from "@/services/friendsService";
import useProfilePosts from "@/hooks/useProfilePosts";
import type { FriendUser } from "@/types";

export default function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const requireLogin = useRequireLogin();
  const { status, user: account } = useAuth();
  const { posts, loadMorePosts, hasMore, error: feedError, loading: loadingPosts } = useProfilePosts(userId);

  const [profileDetail, setProfileDetail] = useState<UserProfileDetail | null>(null);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [friends, setFriends] = useState<FriendUser[]>([]);
  const [mutualFriends, setMutualFriends] = useState<FriendUser[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const friendsScope = useRef("");
  const friendActionLock = useRef(false);
  const activeProfile = useRef(userId);
  activeProfile.current = userId;
  const [friendBusy, setFriendBusy] = useState(false);
  const [friendActionError, setFriendActionError] = useState<string | null>(null);
  const [mutualLoading, setMutualLoading] = useState(false);
  const [mutualError, setMutualError] = useState<string | null>(null);
  const [friendsLoading, setFriendsLoading] = useState(true);
  const [friendsError, setFriendsError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [friendsAttempt, setFriendsAttempt] = useState(0);
  const [showFriendMenu, setShowFriendMenu] = useState<boolean>(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 200) {
        if (hasMore && !loadingPosts) {
          loadMorePosts();
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [hasMore, loadingPosts]);

  useEffect(() => { setCurrentUserId(account?.id || null); }, [account?.id]);

  useEffect(() => {
    let active = true;
    setProfileDetail(null); setLoading(true); setError(null);
    if (!userId) { setError("Không tìm thấy ID người dùng."); setLoading(false); return; }
    getUserProfileDetails(userId).then(detail => { if (active) setProfileDetail(detail); })
      .catch(() => { if (active) setError("Không thể tải hồ sơ người dùng."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [userId, status, account?.id, attempt]);

  useEffect(() => {
    let active = true;
    const scope = `${status}:${account?.id}:${userId}`;
    if (friendsScope.current !== scope) { setFriends([]); setMutualFriends([]); friendsScope.current = scope; }
    setFriendsLoading(true); setFriendsError(null);
    if (!userId || status !== "authenticated") { setFriendsLoading(false); return; }
    getAllFriends(userId).then(items => {
      if (!active) return;
      setFriends(items.map(f => {
        const friend = f.friend?.id === userId ? f.user : f.friend;
        return { id: friend.id, name: friend.displayName || friend.username || "Người dùng", avatar: getUserAvatarUrl(friend), cover: "/default-cover.svg", mutualFriends: 0, location: friend.location || "", work: friend.work || "", education: friend.education || "", bio: friend.bio || "" };
      }));
    }).catch(() => { if (active) setFriendsError("Không tải được danh sách bạn bè."); })
      .finally(() => { if (active) setFriendsLoading(false); });
    return () => { active = false; };
  }, [userId, status, account?.id, friendsAttempt]);

  // 3. Compute real mutual friends if currentUserId & targetUserId exist
  useEffect(() => {
    setMutualError(null);
    if (!currentUserId || !userId || currentUserId === userId || friends.length === 0) {
      setMutualLoading(false);
      setMutualFriends([]);
      return;
    }

    let active = true;
    setMutualLoading(true);
    getAllFriends(currentUserId)
      .then((myFriendships) => {
        const myFriendIds = new Set(
          myFriendships.map((f) => (f.friend?.id === currentUserId ? f.user?.id : f.friend?.id))
        );
        const actualMutual = friends.filter((f) => myFriendIds.has(String(f.id)));
        if (active) setMutualFriends(actualMutual);
      })
      .catch(() => { if (active) setMutualError("Không tải được bạn chung."); })
      .finally(() => { if (active) setMutualLoading(false); });
    return () => { active = false; };
  }, [currentUserId, userId, friends]);

  async function runFriendAction(action: () => Promise<void>) {
    if (!requireLogin() || friendActionLock.current || !profileDetail) return;
    friendActionLock.current = true; setFriendBusy(true); setFriendActionError(null);
    try { await action(); }
    catch { if (activeProfile.current === profileDetail.id) setFriendActionError("Thao tác chưa thành công. Vui lòng thử lại."); }
    finally { friendActionLock.current = false; setFriendBusy(false); }
  }

  const handleToggleFriend = () => runFriendAction(async () => {
    if (!profileDetail || !currentUserId) return;
    let next = profileDetail.friendshipStatus;
    let requestId = profileDetail.friendRequestId;
    if (next === "NONE") {
      requestId = (await sendFriendRequest(currentUserId, profileDetail.id))?.id;
      next = "PENDING_SENT";
    } else if (next === "PENDING_SENT") {
      if (!requestId) throw new Error("Missing friend request");
      await cancelFriendRequest(requestId); next = "NONE";
    } else if (next === "PENDING_RECEIVED") {
      if (!requestId) throw new Error("Missing friend request");
      await acceptFriendRequestById(requestId, currentUserId, profileDetail.id); next = "FRIEND";
    }
    if (activeProfile.current !== profileDetail.id) return;
    setProfileDetail(prev => prev ? { ...prev, friendshipStatus: next, friendRequestId: requestId } : prev);
    setFriendsAttempt(n => n + 1);
  });

  const handleRejectRequest = () => runFriendAction(async () => {
    if (!profileDetail?.friendRequestId) throw new Error("Missing friend request");
    await rejectFriendRequest(profileDetail.friendRequestId);
    if (activeProfile.current === profileDetail.id) setProfileDetail(prev => prev ? { ...prev, friendshipStatus: "NONE" } : prev);
  });

  const handleUnfriend = () => {
    if (!profileDetail || !window.confirm(`Bạn có chắc chắn muốn hủy kết bạn với ${profileDetail.displayName}?`)) return;
    return runFriendAction(async () => {
      await unfriend(profileDetail.id);
      if (activeProfile.current !== profileDetail.id) return;
      setShowFriendMenu(false);
      setProfileDetail(prev => prev ? { ...prev, friendshipStatus: "NONE" } : prev);
      setFriendsAttempt(n => n + 1);
    });
  };

  if (loading || (profileDetail && profileDetail.id !== userId)) return <ProfileSkeleton />;

  if (error || !profileDetail) {
    return (
      <main className="min-h-[calc(100vh-56px)] bg-[#F0F2F5] px-4 py-8">
        <div className="mx-auto max-w-[720px] rounded-xl border border-[#E4E6EB] bg-white p-8 text-center">
          <div className="flex justify-center mb-4"><svg className="w-16 h-16 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg></div>
          <h2 className="text-xl font-bold text-[#1C1E21]">{error || "Không tìm thấy hồ sơ người dùng"}</h2>
          <button onClick={() => setAttempt(n => n + 1)} className="mt-3 text-[#1877F2]">Thử lại</button>
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
      ? resolveApiUrl(`/services/mediaservice/api/media/${profileDetail.coverMediaId}/file`)
      : "/default-cover.svg",
    bio: profileDetail.bio,
    location: profileDetail.location,
    education: profileDetail.education,
    work: profileDetail.workplace,
    relationship: profileDetail.relationship,
  };

  // Target user's posts filter
  const targetUserPosts = posts;

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

      <FollowButton
        user={{
          id: profileDetail.id,
          name: user.name,
          avatar: user.avatar,
          cover: user.cover || "",
          mutualFriends: 0,
          location: user.location || "",
          work: user.work || "",
          education: user.education || "",
          bio: user.bio || "",
        }}
      />

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
      mutualLoading={mutualLoading}
      mutualError={mutualError}
      friendsLoading={friendsLoading}
      friendsError={friendsError}
      retryFriends={() => setFriendsAttempt(n => n + 1)}
      user={user}
      isOwn={isOwn}
      posts={targetUserPosts}
      feedError={feedError} retryFeed={loadMorePosts}
      friends={friends}
      mutualCount={friendsLoading || mutualLoading ? profileDetail.mutualFriendsCount : mutualFriends.length}
      mutualFriends={mutualFriends}
      actionButtons={<fieldset disabled={friendBusy} className="contents">{actionButtons}{friendBusy && <span role="status" className="text-sm text-[#65676B]">Đang xử lý…</span>}{friendActionError && <span role="alert" className="text-sm text-[#B42318]">{friendActionError}</span>}</fieldset>}
      loading={loadingPosts}
      hasMore={hasMore}
    />
  );
}
