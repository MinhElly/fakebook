import { useParams, useNavigate } from "react-router";
import { FRIEND_USERS } from "@/constants/data";
import { useFriendStore } from "@/stores/friendStore";
import { usePostStore } from "@/stores/postStore";
import FriendButton from "@/components/profile/FriendButton";
import FollowButton from "@/components/profile/FollowButton";
import ProfileLayout from "@/components/profile/ProfileLayout";

export default function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { friends } = useFriendStore();
  const { posts } = usePostStore();

  const user = FRIEND_USERS.find(u => u.id === Number(userId));

  if (!user) {
    return (
      <div className="min-h-screen bg-[#F0F2F5] flex items-center justify-center">
        <div className="bg-white rounded-xl p-8 text-center shadow-sm border border-[#E4E6EB]">
          <p className="text-5xl mb-3">😕</p>
          <p className="text-xl font-bold text-[#1C1E21] mb-2">Không tìm thấy người dùng</p>
          <button onClick={() => navigate(-1)}
            className="mt-4 px-4 py-2 bg-[#1877F2] text-white rounded-lg font-semibold hover:bg-[#166FE5] transition-colors text-sm">
            Quay lại
          </button>
        </div>
      </div>
    );
  }

  const mutualFriends = friends.filter(f => f.id !== user.id).slice(0, user.mutualFriends);
  const userPosts = posts.filter(p => p.user === user.name);

  const actionButtons = (
    <>
      <FriendButton user={user} />
      <FollowButton user={user} />
      <button className="flex items-center gap-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] px-3 sm:px-4 py-2 rounded-lg font-semibold text-sm transition-colors">
        <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clipRule="evenodd"/></svg>
        <span className="hidden sm:inline">Nhắn tin</span>
      </button>
      <button className="flex items-center justify-center w-9 h-9 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] rounded-lg transition-colors">
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
      </button>
    </>
  );

  return (
    <ProfileLayout
      user={user}
      isOwn={false}
      posts={userPosts}
      mutualFriends={mutualFriends}
      mutualCount={user.mutualFriends}
      actionButtons={actionButtons}
    />
  );
}
