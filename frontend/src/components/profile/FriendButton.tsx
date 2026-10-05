import { useRef, useState } from "react";
import { useFriendStore } from "@/stores/friendStore";
import { useOutsideClick } from "@/hooks/useOutsideClick";
import type { FriendUser } from "@/types";

interface Props {
  user: FriendUser;
  size?: "sm" | "md";
}

export default function FriendButton({ user, size = "md" }: Props) {
  const { getStatus, sendRequest, cancelRequest, acceptRequest, rejectRequest, removeFriend } = useFriendStore();
  const [showMenu, setShowMenu] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  useOutsideClick(menuRef, () => setShowMenu(false));

  const status = getStatus(user.id);
  const pad = size === "sm" ? "px-3 py-1.5 text-xs" : "px-4 py-2 text-sm";

  if (status === "friends") {
    return (
      <div ref={menuRef} className="relative">
        <button
          onClick={() => setShowMenu(p => !p)}
          className={`flex items-center gap-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold rounded-lg transition-colors ${pad}`}
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v1h8v-1zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-1a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v1h-3zM4.75 12.094A5.973 5.973 0 004 15v1H1v-1a3 3 0 013.75-2.906z"/></svg>
          Bạn bè
          <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
        </button>
        {showMenu && (
          <div className="absolute left-0 top-full mt-1 bg-white rounded-xl shadow-xl border border-[#E4E6EB] w-48 z-20 py-1 overflow-hidden">
            <button
              onClick={() => { removeFriend(user.id); setShowMenu(false); }}
              className="flex items-center gap-2 w-full px-3 py-2.5 hover:bg-[#F0F2F5] text-sm text-red-600 font-medium"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.368zm1.414-1.414L6.524 5.11a6 6 0 018.367 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clipRule="evenodd"/></svg>
              Hủy kết bạn
            </button>
          </div>
        )}
      </div>
    );
  }

  if (status === "pending_sent") {
    return (
      <button
        onClick={() => cancelRequest(user.id)}
        className={`flex items-center gap-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold rounded-lg transition-colors ${pad}`}
      >
        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z"/></svg>
        Đã gửi lời mời
      </button>
    );
  }

  if (status === "pending_received") {
    return (
      <div className="flex gap-2">
        <button
          onClick={() => acceptRequest(user.id)}
          className={`flex items-center gap-1.5 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold rounded-lg transition-colors ${pad}`}
        >
          Xác nhận
        </button>
        <button
          onClick={() => rejectRequest(user.id)}
          className={`flex items-center gap-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] font-semibold rounded-lg transition-colors ${pad}`}
        >
          Xóa
        </button>
      </div>
    );
  }

  return (
    <button
      onClick={() => sendRequest(user)}
      className={`flex items-center gap-1.5 bg-[#E7F3FF] hover:bg-[#DCE8FF] text-[#1877F2] font-semibold rounded-lg transition-colors ${pad}`}
    >
      <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z"/></svg>
      Thêm bạn bè
    </button>
  );
}
