import { useState } from "react";
import { getUserAvatarUrl, type FriendRequestItem } from "@/services/friendsService";

interface SentRequestsModalProps {
  isOpen: boolean;
  onClose: () => void;
  sentRequests: FriendRequestItem[];
  onCancelRequest: (request: FriendRequestItem) => Promise<void>;
  loading: boolean;
}

export default function SentRequestsModal({
  isOpen,
  onClose,
  sentRequests,
  onCancelRequest,
  loading,
}: SentRequestsModalProps) {
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleCancel = async (req: FriendRequestItem) => {
    setCancellingId(req.id);
    try {
      await onCancelRequest(req);
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs animate-fade-in">
      <div className="bg-white rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl flex flex-col max-h-[85vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#E4E6EB]">
          <h2 className="text-lg font-bold text-[#050505]">Lời mời kết bạn đã gửi</h2>
          <button
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors text-[#65676B]"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-4 overflow-y-auto flex-1 divide-y divide-[#E4E6EB]">
          {loading ? (
            <div className="py-12 text-center text-sm text-[#65676B] font-medium">
              Đang tải danh sách lời mời đã gửi...
            </div>
          ) : sentRequests.length === 0 ? (
            <div className="py-12 text-center">
              <div className="w-14 h-14 bg-[#F0F2F5] rounded-full flex items-center justify-center mx-auto mb-3 text-[#65676B]">
                <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </div>
              <p className="text-base font-semibold text-[#050505]">Không có lời mời đã gửi nào</p>
              <p className="text-xs text-[#65676B] mt-1">Khi bạn gửi lời mời kết bạn cho ai đó, danh sách sẽ xuất hiện tại đây.</p>
            </div>
          ) : (
            sentRequests.map((req) => {
              const receiver = req.receiver;
              const avatarUrl = getUserAvatarUrl(receiver);

              const isCancelling = cancellingId === req.id;

              return (
                <div key={req.id} className="py-3 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={avatarUrl}
                      alt={receiver?.displayName || "User"}
                      className="w-12 h-12 rounded-full object-cover bg-[#F0F2F5] flex-shrink-0"
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        if (!target.src.includes("default-avatar.svg")) {
                          target.src = "/default-avatar.svg";
                        }
                      }}
                    />
                    <div className="min-w-0">
                      <p className="font-semibold text-sm text-[#050505] truncate">
                        {receiver?.displayName || receiver?.username || "Người dùng"}
                      </p>
                      <p className="text-xs text-[#65676B]">Đã gửi {new Date(req.createdAt).toLocaleDateString("vi-VN")}</p>
                    </div>
                  </div>

                  <button
                    disabled={isCancelling}
                    onClick={() => handleCancel(req)}
                    className="px-4 py-2 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#050505] font-semibold text-xs rounded-lg transition-colors flex-shrink-0 disabled:opacity-50"
                  >
                    {isCancelling ? "Đang hủy..." : "Hủy lời mời"}
                  </button>
                </div>
              );
            })
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-3 bg-[#F0F2F5] border-t border-[#E4E6EB] flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-[#1877F2] hover:bg-[#166FE5] text-white font-semibold text-sm rounded-lg transition-colors"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
