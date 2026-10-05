import { useEffect } from "react";
import { useNavigate } from "react-router";
import { getUserAvatarUrl, type FriendRequestItem } from "@/services/friendsService";

interface FriendRequestToastProps {
  request: FriendRequestItem;
  onAccept: (request: FriendRequestItem) => void;
  onReject: (request: FriendRequestItem) => void;
  onClose: () => void;
  duration?: number;
}

export default function FriendRequestToast({
  request,
  onAccept,
  onReject,
  onClose,
  duration = 8000,
}: FriendRequestToastProps) {
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);
    return () => clearTimeout(timer);
  }, [onClose, duration]);

  const sender = request.sender;
  const avatarUrl = getUserAvatarUrl(sender);

  const handleProfileClick = () => {
    if (sender && sender.id) {
      navigate(`/profile/${sender.id}`);
      onClose();
    }
  };

  return (
    <div className="fixed bottom-5 left-5 z-[210] w-[360px] max-w-[calc(100vw-40px)] bg-white rounded-2xl border border-[#E4E6EB] shadow-2xl p-4 animate-slide-up">
      {/* Header Notification Title */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-full bg-[#1877F2] text-white flex items-center justify-center font-bold text-xs">
            f
          </div>
          <span className="text-xs font-bold text-[#65676B] uppercase tracking-wide">
            Lời mời kết bạn mới
          </span>
        </div>
        <button
          onClick={onClose}
          className="w-7 h-7 rounded-full hover:bg-[#F0F2F5] text-[#65676B] flex items-center justify-center transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Body Info */}
      <div className="flex items-start gap-3">
        {/* Clickable Avatar */}
        <img
          src={avatarUrl}
          alt={sender?.displayName || "User"}
          onClick={handleProfileClick}
          className="w-12 h-12 rounded-full object-cover bg-[#F0F2F5] flex-shrink-0 cursor-pointer hover:opacity-90 hover:scale-105 transition-all"
          title={`Xem trang cá nhân của ${sender?.displayName || sender?.username}`}
          onError={(e) => {
            const target = e.target as HTMLImageElement;
            if (!target.src.includes("default-avatar.svg")) {
              target.src = "/default-avatar.svg";
            }
          }}
        />

        <div className="flex-1 min-w-0">
          {/* Clickable Name */}
          <p
            onClick={handleProfileClick}
            className="text-sm font-semibold text-[#050505] hover:text-[#1877F2] hover:underline cursor-pointer leading-tight truncate"
            title={`Xem trang cá nhân của ${sender?.displayName || sender?.username}`}
          >
            {sender?.displayName || sender?.username || "Một người dùng"}
          </p>
          <p className="text-xs text-[#65676B] mt-0.5">Đã gửi cho bạn một lời mời kết bạn.</p>

          {/* Quick Action Buttons */}
          <div className="flex items-center gap-2 mt-3">
            <button
              onClick={() => {
                onAccept(request);
                onClose();
              }}
              className="flex-1 py-1.5 bg-[#1877F2] hover:bg-[#166FE5] text-white text-xs font-semibold rounded-lg transition-colors text-center"
            >
              Chấp nhận
            </button>
            <button
              onClick={() => {
                onReject(request);
                onClose();
              }}
              className="flex-1 py-1.5 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] text-xs font-semibold rounded-lg transition-colors text-center"
            >
              Xóa
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
