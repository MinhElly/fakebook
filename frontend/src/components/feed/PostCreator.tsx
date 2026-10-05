import { useState } from "react";
import { useUserStore } from "@/stores/userStore";
import CreatePostModal from "./CreatePostModal";

export default function PostCreator() {
  const { profile } = useUserStore();
  const [showModal, setShowModal] = useState(false);

  const firstName = profile.name ? profile.name.trim().split(" ").pop() : "";

  return (
    <>
      <div className="mb-3 flex items-center gap-3 rounded-xl border border-[#E4E6EB] bg-white
  px-4 py-3 shadow-sm">
        <img src={profile.avatar} alt="Avatar" className="h-10 w-10 flex-shrink-0 rounded-full
  object-cover" />
        <button
          onClick={() => setShowModal(true)}
          className="h-10 flex-1 rounded-full bg-[#F0F2F5] hover:bg-[#E4E6EB] transition-colors
  px-4 text-left text-sm text-[#65676B]"
        >
          {firstName} ơi, bạn đang nghĩ gì thế?
        </button>
      </div>

      {/* Hiển thị Khung đăng bài khi showModal = true */}
      {showModal && (
        <CreatePostModal onClose={() => setShowModal(false)} />
      )}
    </>
  );
}