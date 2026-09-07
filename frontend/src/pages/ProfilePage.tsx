import { useState } from "react";
import EditProfileModal from "@/components/profile/EditProfileModal";
import ProfileLayout from "@/components/profile/ProfileLayout";
import { usePostStore } from "@/stores/postStore";
import { useUserStore } from "@/stores/userStore";
import { useFriendStore } from "@/stores/friendStore";

export default function ProfilePage() {
  const { posts } = usePostStore();
  const { profile } = useUserStore();
  const { friends } = useFriendStore();
  const [showEditModal, setShowEditModal] = useState(false);

  const myPosts = posts.filter(p => p.user === profile.name);

  const actionButtons = (
    <>
      <button className="flex items-center gap-2 bg-[#1877F2] hover:bg-[#166FE5] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors">
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd"/></svg>
        Thêm vào tin
      </button>
      <button onClick={() => setShowEditModal(true)}
        className="flex items-center gap-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-sm font-semibold px-4 py-2 rounded-lg transition-colors">
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
        Chỉnh sửa trang cá nhân
      </button>
    </>
  );

  return (
    <>
      <ProfileLayout
        user={profile}
        isOwn={true}
        posts={myPosts}
        friends={friends}
        actionButtons={actionButtons}
        onEditCover={() => setShowEditModal(true)}
        onEditAvatar={() => setShowEditModal(true)}
      />
      {showEditModal && <EditProfileModal onClose={() => setShowEditModal(false)} />}
    </>
  );
}
