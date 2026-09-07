import { useState } from "react";
import { useUserStore } from "@/stores/userStore";
import CreatePostModal from "./CreatePostModal";

export default function PostCreator() {
  const { profile } = useUserStore();
  const [showModal, setShowModal] = useState(false);

  return (
    <>
      <div className="bg-white rounded-xl shadow-sm border border-[#E4E6EB] px-4 py-3 mb-3">
        <div className="flex items-center gap-2">
          <img src={profile.avatar} alt="me" className="w-10 h-10 rounded-full object-cover flex-shrink-0" />
          <button
            onClick={() => setShowModal(true)}
            className="flex-1 bg-[#F0F2F5] hover:bg-[#E4E6EB] rounded-full px-4 h-10 text-left text-[#65676B] text-sm transition-colors"
          >
            {profile.name.split(" ").pop()} ơi, bạn đang nghĩ gì thế?
          </button>
          <div className="flex items-center gap-1 flex-shrink-0">
            <button
              onClick={() => setShowModal(true)}
              className="w-10 h-10 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors"
              title="Video trực tiếp"
            >
              <svg className="w-6 h-6 text-red-500" fill="currentColor" viewBox="0 0 24 24">
                <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>
              </svg>
            </button>
            <button
              onClick={() => setShowModal(true)}
              className="w-10 h-10 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors"
              title="Ảnh/video"
            >
              <svg className="w-6 h-6 text-green-500" fill="currentColor" viewBox="0 0 24 24">
                <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
              </svg>
            </button>
            <button
              onClick={() => setShowModal(true)}
              className="w-10 h-10 rounded-full hover:bg-[#F0F2F5] flex items-center justify-center transition-colors"
              title="Cảm xúc/hoạt động"
            >
              <svg className="w-6 h-6 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
                <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      {showModal && <CreatePostModal onClose={() => setShowModal(false)} />}
    </>
  );
}
